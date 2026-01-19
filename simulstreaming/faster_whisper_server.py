#!/usr/bin/env python3
"""
Faster Whisper Streaming Server - Server sử dụng faster-whisper với float16.
Thay thế cho simulstreaming_whisper_server.py với hiệu năng tốt hơn.

Cách chạy:
    python faster_whisper_server.py --model_size small --compute_type float16 --lan vi

Các tham số phổ biến:
    --model_size: tiny, base, small, medium, large-v2, large-v3, turbo
    --compute_type: float16 (GPU), int8 (CPU/GPU), float32 (CPU)
    --lan: Mã ngôn ngữ (vi, en, ja, zh, ...) hoặc 'auto'
    --device: cuda hoặc cpu
    
Ví dụ:
    # Chạy với model small, float16 trên GPU
    python faster_whisper_server.py --model_size small --compute_type float16 --lan vi
    
    # Chạy với model large-v3, int8 trên GPU (tiết kiệm VRAM)
    python faster_whisper_server.py --model_size large-v3 --compute_type int8_float16 --lan vi
    
    # Chạy với model medium trên CPU
    python faster_whisper_server.py --model_size medium --compute_type int8 --device cpu --lan vi
"""

from faster_whisper_streaming import faster_whisper_factory, faster_whisper_args
from whisper_streaming.whisper_server import main_server

if __name__ == "__main__":
    main_server(faster_whisper_factory, add_args=faster_whisper_args)
