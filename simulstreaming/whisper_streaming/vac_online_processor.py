from whisper_streaming.base import OnlineProcessorInterface
from whisper_streaming.silero_vad_iterator import FixedVADIterator
import numpy as np
import time

import logging
logger = logging.getLogger(__name__)
import sys

class VACOnlineASRProcessor(OnlineProcessorInterface):
    '''Wraps OnlineASRProcessor with VAC (Voice Activity Controller).

    It works the same way as OnlineASRProcessor: it receives chunks of audio (e.g. 0.04 seconds),
    it runs VAD and continuously detects whether there is speech or not.
    When it detects end of speech (non-voice for 500ms), it makes OnlineASRProcessor to end the utterance immediately.
    '''

    SILENCE_THRESHOLD = 3.0  # seconds - if silence exceeds this, add newline

    def __init__(self, online_chunk_size, online, min_buffered_length=1):
        self.online_chunk_size = online_chunk_size
        self.online = online

        self.min_buffered_frames = int(min_buffered_length * self.SAMPLING_RATE)

        # VAC:
        import torch
        model, _ = torch.hub.load(
            repo_or_dir='snakers4/silero-vad',
            model='silero_vad'
        )
        self.vac = FixedVADIterator(model)  # we use the default options there: 500ms silence, 100ms padding, etc.

        self.init()

    def init(self):
        self.online.init()
        self.vac.reset_states()
        self.current_online_chunk_buffer_size = 0

        self.is_currently_final = False

        self.status = None  # or "voice" or "nonvoice"
        self.audio_buffer = np.array([],dtype=np.float32)
        self.buffer_offset = 0  # in frames
        
        # Silence detection for newline insertion
        self.silence_start_time = None  # Time when silence started
        self.newline_pending = False  # Flag to add newline on next output

    def clear_buffer(self):
        self.audio_buffer = np.array([],dtype=np.float32)

    def insert_audio_chunk(self, audio):
        res = self.vac(audio)
        self.audio_buffer = np.append(self.audio_buffer, audio)
        if res is not None:
            frame = list(res.values())[0] - self.buffer_offset
            frame = max(0, frame)
            if 'start' in res and 'end' not in res:
                self.status = 'voice'
                # Voice detected - check if we need to add newline due to long silence
                if self.silence_start_time is not None:
                    silence_duration = time.time() - self.silence_start_time
                    if silence_duration >= self.SILENCE_THRESHOLD:
                        self.newline_pending = True
                        logger.info(f"Silence exceeded {self.SILENCE_THRESHOLD}s ({silence_duration:.2f}s), newline will be added")
                self.silence_start_time = None  # Reset silence timer
                
                send_audio = self.audio_buffer[frame:]
                self.online.init(offset=(frame + self.buffer_offset)/self.SAMPLING_RATE)
                self.online.insert_audio_chunk(send_audio)
                self.current_online_chunk_buffer_size += len(send_audio)
                self.buffer_offset += len(self.audio_buffer)
                self.clear_buffer()
            elif 'end' in res and 'start' not in res:
                self.status = 'nonvoice'
                # Start silence timer when voice ends
                if self.silence_start_time is None:
                    self.silence_start_time = time.time()
                    
                if frame > 0:
                    send_audio = self.audio_buffer[:frame]
                    self.online.insert_audio_chunk(send_audio)
                    self.current_online_chunk_buffer_size += len(send_audio)
                self.is_currently_final = True
                keep_frames = min(len(self.audio_buffer) - frame, self.min_buffered_frames)
                self.buffer_offset += len(self.audio_buffer) - keep_frames
                self.audio_buffer = self.audio_buffer[-keep_frames:]
            else:
                beg = max(0, res["start"] - self.buffer_offset)
                end = max(0, res["end"] - self.buffer_offset)
                self.status = 'nonvoice'
                # Start silence timer when voice ends
                if self.silence_start_time is None:
                    self.silence_start_time = time.time()
                    
                if beg < end:
                    send_audio = self.audio_buffer[beg:end]
                    self.online.init(offset=((beg + self.buffer_offset)/self.SAMPLING_RATE))
                    self.online.insert_audio_chunk(send_audio)
                    self.current_online_chunk_buffer_size += len(send_audio)
                self.is_currently_final = True
                keep_frames = min(len(self.audio_buffer) - end, self.min_buffered_frames)
                self.buffer_offset += len(self.audio_buffer) - keep_frames
                self.audio_buffer = self.audio_buffer[-keep_frames:]
        else:
            if self.status == 'voice':
                self.silence_start_time = None  # Reset silence timer during voice
                self.online.insert_audio_chunk(self.audio_buffer)
                self.current_online_chunk_buffer_size += len(self.audio_buffer)
                self.buffer_offset += len(self.audio_buffer)
                self.clear_buffer()
            else:
                # Track silence duration when in nonvoice status
                if self.status == 'nonvoice' and self.silence_start_time is None:
                    self.silence_start_time = time.time()
                # We keep 1 second because VAD may later find start of voice in it.
                # But we trim it to prevent OOM.
                self.buffer_offset += max(0, len(self.audio_buffer) - self.min_buffered_frames)
                self.audio_buffer = self.audio_buffer[-self.min_buffered_frames:]

    def process_iter(self):
        if self.is_currently_final:
            return self.finish()
        elif self.current_online_chunk_buffer_size > self.SAMPLING_RATE*self.online_chunk_size:
            self.current_online_chunk_buffer_size = 0
            ret = self.online.process_iter()
            # Add newline prefix if pending (silence exceeded threshold)
            if self.newline_pending and ret and ret.get('text'):
                ret['text'] = '\n' + ret['text']
                self.newline_pending = False
                logger.info("Newline added to transcript due to long silence")
            return ret
        else:
            logger.info(f"no online update, only VAD. {self.status}")
            return {}

    def finish(self):
        ret = self.online.finish()
        # Add newline prefix if pending (silence exceeded threshold)
        if self.newline_pending and ret and ret.get('text'):
            ret['text'] = '\n' + ret['text']
            self.newline_pending = False
            logger.info("Newline added to transcript due to long silence")
        self.current_online_chunk_buffer_size = 0
        self.is_currently_final = False
        return ret