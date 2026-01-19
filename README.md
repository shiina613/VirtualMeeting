# VirtualMeeting

Dự án VirtualMeeting là một hệ thống quản lý cuộc họp ảo tích hợp trợ lý ảo (virtual secretary) và xử lý luồng âm thanh thời gian thực (simulstreaming) để chuyển đổi giọng nói thành văn bản và dịch thuật.

## Mô tả

Dự án bao gồm hai thành phần chính:

- **Secretary**: Backend API được xây dựng bằng Spring Boot để quản lý thông tin cuộc họp, phòng họp, và phòng ban.
- **SimulStreaming**: Hệ thống xử lý âm thanh thời gian thực sử dụng mô hình Whisper để chuyển đổi giọng nói thành văn bản (transcription) và dịch thuật (translation) trong chế độ đồng thời (simultaneous mode).

## Cơ chế hoạt động

### Secretary (Backend)
- **Công nghệ**: Spring Boot, Spring Data JPA, MySQL, Swagger/OpenAPI.
- **Chức năng**:
  - Quản lý CRUD cho các thực thể: Meeting (cuộc họp), Room (phòng họp), Department (phòng ban).
  - Cung cấp REST API để tạo, đọc, cập nhật, xóa dữ liệu.
  - Tích hợp Swagger UI để tài liệu hóa và test API.
  - Xử lý ngoại lệ và validation dữ liệu.
- **Cơ sở dữ liệu**: MySQL với cấu hình JPA/Hibernate tự động tạo bảng.

### SimulStreaming (Speech Processing)
- **Công nghệ**: Python, Whisper model, Faster Whisper, Simul-Whisper, Whisper-Streaming.
- **Chức năng**:
  - Xử lý âm thanh thời gian thực từ microphone hoặc file audio.
  - Sử dụng chính sách AlignAtt để tối ưu hóa độ trễ và hiệu suất trong chế độ simultaneous.
  - Hỗ trợ transcription (chuyển văn bản) và translation (dịch thuật) với nhiều ngôn ngữ.
  - Tích hợp Voice Activity Detection (VAD) để phát hiện hoạt động giọng nói.
  - Server TCP để nhận luồng âm thanh và xuất kết quả transcription/translation.
- **Mô hình**: Sử dụng Whisper large-v3 hoặc các biến thể khác, với beam search và prompt để cải thiện chất lượng.

Hai thành phần có thể tích hợp để tạo một hệ thống họp ảo hoàn chỉnh, nơi Secretary quản lý metadata và SimulStreaming xử lý nội dung âm thanh.

## Cách chạy code

### Yêu cầu hệ thống
- Java 21 (cho Secretary)
- Python 3.8+ (cho SimulStreaming)
- MySQL 8.0+
- Git

### Secretary (Backend)

1. **Cài đặt MySQL**:
   - Tạo database: `meeting_management`
   - Cập nhật thông tin kết nối trong `secretary/src/main/resources/application.properties` nếu cần.

2. **Chạy ứng dụng**:
   ```bash
   cd secretary
   ./mvnw spring-boot:run
   ```
   Hoặc sử dụng IDE: Chạy class `VirtualSecretaryApplication.java`.

3. **Truy cập**:
   - API: http://localhost:8080/api/
   - Swagger UI: http://localhost:8080/swagger-ui.html

### SimulStreaming (Speech Processing)

1. **Cài đặt dependencies**:
   ```bash
   cd simulstreaming
   pip install -r requirements.txt
   ```

2. **Chạy từ file audio (simulation)**:
   ```bash
   python simulstreaming_whisper.py audio.wav --language en --task transcribe
   ```
   Thay `audio.wav` bằng file WAV 16kHz mono.

3. **Chạy server cho real-time từ microphone**:
   ```bash
   python simulstreaming_whisper_server.py --host localhost --port 43001 --language en --task transcribe
   ```
   Sau đó gửi audio qua TCP:
   - Linux: `arecord -f S16_LE -c1 -r 16000 -t raw -D default | nc localhost 43001`
   - Windows/Mac: Sử dụng ffmpeg hoặc các giải pháp thay thế.

4. **Tùy chọn nâng cao**:
   - Thêm `--vac` để sử dụng Voice Activity Controller.
   - Thêm `--beams 5` cho beam search.
   - Xem thêm options trong `python simulstreaming_whisper.py -h`.

### Tích hợp cả hai thành phần
- Chạy Secretary backend để quản lý dữ liệu cuộc họp.
- Chạy SimulStreaming để xử lý âm thanh và gửi kết quả transcription về backend qua API.

## Dependencies

### Secretary
- Spring Boot 4.0.1
- Spring Web, Spring Data JPA
- MySQL Connector
- Lombok
- SpringDoc OpenAPI (Swagger)

### SimulStreaming
- PyTorch
- OpenAI Whisper
- Faster Whisper
- NumPy, SciPy
- Torchaudio (cho VAD)
- Xem chi tiết trong `simulstreaming/requirements.txt`

## API Documentation
Secretary cung cấp tài liệu API đầy đủ qua Swagger UI tại http://localhost:8080/swagger-ui.html sau khi chạy.

## License
- Secretary: MIT (giả định)
- SimulStreaming: MIT

## Liên hệ
Nếu có câu hỏi hoặc cần hỗ trợ, vui lòng tạo issue trên GitHub hoặc liên hệ tác giả.