#!/usr/bin/env python3
"""
Faster Whisper Streaming - Sử dụng faster-whisper với float16 cho streaming ASR.
Thay thế cho simulstreaming_whisper.py với hiệu năng tốt hơn.
"""

from whisper_streaming.base import OnlineProcessorInterface, ASRBase
import argparse
import sys
import logging
import numpy as np
from typing import Optional, List, Dict, Any

logger = logging.getLogger(__name__)

# Kiểm tra faster-whisper có được cài đặt không
try:
    from faster_whisper import WhisperModel
    FASTER_WHISPER_AVAILABLE = True
except ImportError:
    FASTER_WHISPER_AVAILABLE = False
    logger.warning("faster-whisper not installed. Install with: pip install faster-whisper")


def faster_whisper_args(parser):
    """Thêm arguments cho Faster Whisper"""
    group = parser.add_argument_group('Faster Whisper arguments')
    group.add_argument('--model_size', type=str, default='small',
                       choices=['tiny', 'tiny.en', 'base', 'base.en', 'small', 'small.en', 
                                'medium', 'medium.en', 'large-v1', 'large-v2', 'large-v3', 
                                'large-v3-turbo', 'turbo', 'distil-large-v2', 'distil-large-v3'],
                       help='Kích thước model Whisper. Mặc định: small')
    group.add_argument('--model_path', type=str, default=None,
                       help='Đường dẫn tới model đã tải. Nếu không có, model sẽ được tải tự động.')
    group.add_argument('--device', type=str, default='cuda',
                       choices=['cuda', 'cpu', 'auto'],
                       help='Thiết bị để chạy model. Mặc định: cuda')
    group.add_argument('--compute_type', type=str, default='float16',
                       choices=['float16', 'float32', 'int8', 'int8_float16', 'int8_float32'],
                       help='Loại compute type. Mặc định: float16')
    group.add_argument('--device_index', type=int, default=0,
                       help='Index của GPU. Mặc định: 0')
    group.add_argument('--cpu_threads', type=int, default=4,
                       help='Số luồng CPU khi chạy trên CPU. Mặc định: 4')
    
    group = parser.add_argument_group('Audio buffer')
    group.add_argument('--audio_max_len', type=float, default=30.0,
                       help='Độ dài tối đa của audio buffer (giây). Mặc định: 30.0')
    group.add_argument('--audio_min_len', type=float, default=1.0,
                       help='Độ dài tối thiểu audio trước khi xử lý (giây). Mặc định: 1.0')
    
    group = parser.add_argument_group('Transcription options')
    group.add_argument('--beam_size', type=int, default=5,
                       help='Beam size cho beam search. Mặc định: 5')
    group.add_argument('--best_of', type=int, default=5,
                       help='Số lượng candidates để sampling. Mặc định: 5')
    group.add_argument('--patience', type=float, default=1.0,
                       help='Patience cho beam search. Mặc định: 1.0')
    group.add_argument('--temperature', type=float, default=0.0,
                       help='Temperature cho sampling. 0 = greedy. Mặc định: 0.0')
    group.add_argument('--init_prompt', type=str, default=None,
                       help='Prompt khởi tạo cho model.')
    group.add_argument('--condition_on_previous_text', action='store_true', default=True,
                       help='Sử dụng text trước làm context.')
    group.add_argument('--no_condition_on_previous_text', dest='condition_on_previous_text', 
                       action='store_false',
                       help='Không sử dụng text trước làm context.')
    
    group = parser.add_argument_group('VAD options')
    group.add_argument('--vad_filter', action='store_true', default=False,
                       help='Sử dụng VAD filter của Silero. Mặc định: False')
    group.add_argument('--vad_threshold', type=float, default=0.5,
                       help='Ngưỡng VAD. Mặc định: 0.5')
    group.add_argument('--min_silence_duration_ms', type=int, default=2000,
                       help='Thời gian im lặng tối thiểu (ms). Mặc định: 2000')
    
    group = parser.add_argument_group('Anti-hallucination')
    group.add_argument('--no_speech_threshold', '--nonspeech_prob', type=float, default=0.6,
                       help='Ngưỡng no-speech (tương đương --nonspeech_prob). Mặc định: 0.6')
    group.add_argument('--log_prob_threshold', '--logprob_threshold', type=float, default=-1.0,
                       help='Ngưỡng log probability. Mặc định: -1.0')
    group.add_argument('--compression_ratio_threshold', type=float, default=2.4,
                       help='Ngưỡng compression ratio. Mặc định: 2.4')
    group.add_argument('--repetition_penalty', '--max_repeat_tokens', type=float, default=1.0,
                       help='Penalty cho repetition (tương đương --max_repeat_tokens). Mặc định: 1.0')


class FasterWhisperASR(ASRBase):
    """ASR class sử dụng faster-whisper với float16"""
    
    sep = ""  # faster-whisper tự thêm space khi cần
    
    def __init__(self, language: str, model_size: str = 'small', model_path: str = None,
                 device: str = 'cuda', compute_type: str = 'float16', device_index: int = 0,
                 cpu_threads: int = 4, audio_max_len: float = 30.0, audio_min_len: float = 1.0,
                 beam_size: int = 5, best_of: int = 5, patience: float = 1.0, temperature: float = 0.0,
                 init_prompt: str = None, condition_on_previous_text: bool = True,
                 vad_filter: bool = False, vad_threshold: float = 0.5,
                 min_silence_duration_ms: int = 2000, no_speech_threshold: float = 0.6,
                 log_prob_threshold: float = -1.0, compression_ratio_threshold: float = 2.4,
                 repetition_penalty: float = 1.0, task: str = 'transcribe', **kwargs):
        
        if not FASTER_WHISPER_AVAILABLE:
            raise ImportError("faster-whisper not installed. Install with: pip install faster-whisper")
        
        self.language = language if language != 'auto' else None
        self.model_size = model_size
        self.model_path = model_path
        self.device = device
        self.compute_type = compute_type
        self.device_index = device_index
        self.cpu_threads = cpu_threads
        self.audio_max_len = audio_max_len
        self.audio_min_len = audio_min_len
        self.beam_size = beam_size
        self.best_of = best_of
        self.patience = patience
        self.temperature = temperature
        self.init_prompt = init_prompt
        self.condition_on_previous_text = condition_on_previous_text
        self.vad_filter = vad_filter
        self.vad_threshold = vad_threshold
        self.min_silence_duration_ms = min_silence_duration_ms
        self.no_speech_threshold = no_speech_threshold
        self.log_prob_threshold = log_prob_threshold
        self.compression_ratio_threshold = compression_ratio_threshold
        self.repetition_penalty = repetition_penalty
        self.task = task
        
        # Load model
        self.model = self.load_model()
        logger.info(f"Faster Whisper model loaded: {model_size} on {device} with {compute_type}")
        print(f"[INFO] Faster Whisper model loaded: {model_size} on {device} with {compute_type}")
    
    def load_model(self, modelsize=None, cache_dir=None, model_dir=None):
        """Load faster-whisper model"""
        model_size_or_path = self.model_path if self.model_path else self.model_size
        
        # Xử lý device
        if self.device == 'auto':
            device = 'cuda' if self._cuda_available() else 'cpu'
        else:
            device = self.device
        
        # Nếu dùng CPU, compute_type phải là float32 hoặc int8
        compute_type = self.compute_type
        if device == 'cpu' and compute_type == 'float16':
            logger.warning("float16 not supported on CPU, switching to int8")
            compute_type = 'int8'
        
        model = WhisperModel(
            model_size_or_path=model_size_or_path,
            device=device,
            device_index=self.device_index,
            compute_type=compute_type,
            cpu_threads=self.cpu_threads,
            download_root=cache_dir or model_dir,
        )
        
        return model
    
    def _cuda_available(self):
        """Check if CUDA is available"""
        try:
            import torch
            return torch.cuda.is_available()
        except ImportError:
            return False
    
    def transcribe(self, audio: np.ndarray, init_prompt: str = "") -> List[Dict]:
        """
        Transcribe audio array.
        
        Args:
            audio: Audio array (float32, 16kHz)
            init_prompt: Initial prompt
            
        Returns:
            List of segments with word-level timestamps
        """
        prompt = init_prompt or self.init_prompt
        
        segments, info = self.model.transcribe(
            audio,
            language=self.language,
            task=self.task,
            beam_size=self.beam_size,
            best_of=self.best_of,
            patience=self.patience,
            temperature=self.temperature,
            initial_prompt=prompt,
            condition_on_previous_text=self.condition_on_previous_text,
            vad_filter=self.vad_filter,
            vad_parameters={
                "threshold": self.vad_threshold,
                "min_silence_duration_ms": self.min_silence_duration_ms,
            } if self.vad_filter else None,
            no_speech_threshold=self.no_speech_threshold,
            log_prob_threshold=self.log_prob_threshold,
            compression_ratio_threshold=self.compression_ratio_threshold,
            repetition_penalty=self.repetition_penalty,
            word_timestamps=True,
        )
        
        # Detect language if auto
        if self.language is None and info.language:
            self.detected_language = info.language
            logger.info(f"Detected language: {info.language} (prob: {info.language_probability:.2f})")
        
        # Convert generator to list
        result = []
        for segment in segments:
            seg_dict = {
                'id': segment.id,
                'start': segment.start,
                'end': segment.end,
                'text': segment.text,
                'tokens': list(segment.tokens) if segment.tokens else [],
                'avg_logprob': segment.avg_logprob,
                'no_speech_prob': segment.no_speech_prob,
                'words': []
            }
            
            if segment.words:
                for word in segment.words:
                    seg_dict['words'].append({
                        'word': word.word,
                        'start': word.start,
                        'end': word.end,
                        'probability': word.probability
                    })
            
            result.append(seg_dict)
        
        return result
    
    def warmup(self, audio: np.ndarray, init_prompt: str = ""):
        """Warmup model với audio ngắn"""
        try:
            # Tạo audio ngắn 1 giây để warmup
            warmup_audio = np.zeros(16000, dtype=np.float32)
            self.transcribe(warmup_audio)
            logger.info("Model warmup completed")
        except Exception as e:
            logger.warning(f"Warmup failed: {e}")
    
    def use_vad(self):
        """Return whether VAD is being used"""
        return self.vad_filter
    
    def set_translate_task(self):
        """Set task to translate"""
        self.task = 'translate'


class FasterWhisperOnline(OnlineProcessorInterface):
    """Online processor cho Faster Whisper streaming"""
    
    SAMPLING_RATE = 16000
    
    def __init__(self, asr: FasterWhisperASR):
        self.asr = asr
        self.model = asr.model
        self.init()
    
    def init(self, offset: float = None):
        """Initialize/reset the processor"""
        self.audio_buffer = np.array([], dtype=np.float32)
        self.offset = offset if offset is not None else 0.0
        self.is_last = False
        self.beg = self.offset
        self.end = self.offset
        self.last_ts = -1.0
        self.previous_text = ""  # Để sử dụng làm context
        self.buffer_offset = 0.0  # Offset của buffer hiện tại
    
    def insert_audio_chunk(self, audio: np.ndarray):
        """Thêm audio chunk vào buffer"""
        if audio is None or len(audio) == 0:
            return
        
        # Convert to float32 if needed
        if audio.dtype != np.float32:
            audio = audio.astype(np.float32)
        
        self.audio_buffer = np.concatenate([self.audio_buffer, audio])
        
        # Trim buffer nếu quá dài
        max_samples = int(self.asr.audio_max_len * self.SAMPLING_RATE)
        if len(self.audio_buffer) > max_samples:
            # Cắt phần đầu
            trim_samples = len(self.audio_buffer) - max_samples
            self.audio_buffer = self.audio_buffer[trim_samples:]
            self.buffer_offset += trim_samples / self.SAMPLING_RATE
    
    def process_iter(self) -> Dict[str, Any]:
        """
        Process current audio buffer.
        
        Returns:
            Dict with keys: start, end, text, words (or empty dict if not enough audio)
        """
        # Kiểm tra đủ audio chưa
        buffer_duration = len(self.audio_buffer) / self.SAMPLING_RATE
        
        if buffer_duration < self.asr.audio_min_len and not self.is_last:
            logger.debug(f"Buffer too short: {buffer_duration:.2f}s < {self.asr.audio_min_len}s")
            return {}
        
        if len(self.audio_buffer) == 0:
            return {}
        
        # Transcribe
        try:
            segments = self.asr.transcribe(
                self.audio_buffer,
                init_prompt=self.previous_text if self.asr.condition_on_previous_text else None
            )
        except Exception as e:
            logger.error(f"Transcription error: {e}")
            return {}
        
        if not segments:
            return {}
        
        # Combine all segments
        all_text = ""
        all_words = []
        min_start = float('inf')
        max_end = 0.0
        
        for seg in segments:
            all_text += seg['text']
            
            if seg['words']:
                for word in seg['words']:
                    # Adjust timestamps with buffer offset
                    word_data = {
                        'text': word['word'],
                        'start': word['start'] + self.buffer_offset + self.offset,
                        'end': word['end'] + self.buffer_offset + self.offset,
                        'probability': word.get('probability', 1.0)
                    }
                    all_words.append(word_data)
                    min_start = min(min_start, word_data['start'])
                    max_end = max(max_end, word_data['end'])
        
        if not all_text.strip():
            return {}
        
        # Update previous text for context
        self.previous_text = all_text.strip()
        
        # Calculate timestamps
        if all_words:
            self.beg = min_start
            self.end = max_end
        else:
            # Fallback nếu không có word timestamps
            self.beg = self.buffer_offset + self.offset
            self.end = self.beg + buffer_duration
        
        # Ensure non-decreasing timestamps
        self.beg = max(self.beg, self.last_ts + 0.001)
        self.end = max(self.end, self.beg + 0.001)
        self.last_ts = self.end
        
        return {
            'start': self.beg,
            'end': self.end,
            'text': all_text.strip(),
            'words': all_words
        }
    
    def finish(self) -> Dict[str, Any]:
        """
        Process remaining audio and finish.
        
        Returns:
            Final transcription result
        """
        logger.info("Finishing transcription")
        self.is_last = True
        result = self.process_iter()
        
        # Reset for next session
        self.is_last = False
        self.audio_buffer = np.array([], dtype=np.float32)
        self.previous_text = ""
        self.buffer_offset = 0.0
        
        return result


def faster_whisper_factory(args):
    """Factory function để tạo FasterWhisperASR và FasterWhisperOnline"""
    logger.setLevel(args.log_level if hasattr(args, 'log_level') else logging.INFO)
    
    # Collect arguments
    asr_kwargs = {
        'language': args.lan if hasattr(args, 'lan') else args.language if hasattr(args, 'language') else 'auto',
        'model_size': args.model_size if hasattr(args, 'model_size') else 'small',
        'model_path': args.model_path if hasattr(args, 'model_path') else None,
        'device': args.device if hasattr(args, 'device') else 'cuda',
        'compute_type': args.compute_type if hasattr(args, 'compute_type') else 'float16',
        'device_index': args.device_index if hasattr(args, 'device_index') else 0,
        'cpu_threads': args.cpu_threads if hasattr(args, 'cpu_threads') else 4,
        'audio_max_len': args.audio_max_len if hasattr(args, 'audio_max_len') else 30.0,
        'audio_min_len': args.audio_min_len if hasattr(args, 'audio_min_len') else 1.0,
        'beam_size': args.beam_size if hasattr(args, 'beam_size') else 5,
        'best_of': args.best_of if hasattr(args, 'best_of') else 5,
        'patience': args.patience if hasattr(args, 'patience') else 1.0,
        'temperature': args.temperature if hasattr(args, 'temperature') else 0.0,
        'init_prompt': args.init_prompt if hasattr(args, 'init_prompt') else None,
        'condition_on_previous_text': args.condition_on_previous_text if hasattr(args, 'condition_on_previous_text') else True,
        'vad_filter': args.vad_filter if hasattr(args, 'vad_filter') else False,
        'vad_threshold': args.vad_threshold if hasattr(args, 'vad_threshold') else 0.5,
        'min_silence_duration_ms': args.min_silence_duration_ms if hasattr(args, 'min_silence_duration_ms') else 2000,
        'no_speech_threshold': args.no_speech_threshold if hasattr(args, 'no_speech_threshold') else 0.6,
        'log_prob_threshold': args.log_prob_threshold if hasattr(args, 'log_prob_threshold') else -1.0,
        'compression_ratio_threshold': args.compression_ratio_threshold if hasattr(args, 'compression_ratio_threshold') else 2.4,
        'repetition_penalty': args.repetition_penalty if hasattr(args, 'repetition_penalty') else 1.0,
        'task': args.task if hasattr(args, 'task') else 'transcribe',
    }
    
    logger.info(f"Creating Faster Whisper ASR with args: {asr_kwargs}")
    
    asr = FasterWhisperASR(**asr_kwargs)
    online = FasterWhisperOnline(asr)
    
    return asr, online


if __name__ == "__main__":
    # Test standalone
    from whisper_streaming.whisper_server import main_server
    main_server(faster_whisper_factory, add_args=faster_whisper_args)
