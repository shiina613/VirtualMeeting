// API Base URL
const API_BASE = '/api';

// Data cache
let departments = [];
let rooms = [];
let meetings = [];

// Bootstrap instances
let meetingModal, departmentModal, roomModal, toast;

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    // Initialize Bootstrap components
    meetingModal = new bootstrap.Modal(document.getElementById('meetingModal'));
    departmentModal = new bootstrap.Modal(document.getElementById('departmentModal'));
    roomModal = new bootstrap.Modal(document.getElementById('roomModal'));
    toast = new bootstrap.Toast(document.getElementById('toast'));

    // Setup navigation
    setupNavigation();

    // Setup sidebar toggle
    document.getElementById('sidebarToggle').addEventListener('click', toggleSidebar);

    // Update current date/time
    updateDateTime();
    setInterval(updateDateTime, 60000);

    // Load initial data
    loadAllData();
});

// Navigation
function setupNavigation() {
    document.querySelectorAll('.sidebar-nav .nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = link.dataset.page;
            showPage(page);

            // Update active state
            document.querySelectorAll('.sidebar-nav .nav-link').forEach(l => l.classList.remove('active'));
            link.classList.add('active');

            // Close sidebar on mobile
            if (window.innerWidth < 992) {
                document.getElementById('sidebar').classList.remove('show');
            }
        });
    });
}

function showPage(page) {
    // Hide all pages
    document.querySelectorAll('.page-content').forEach(p => p.classList.add('d-none'));

    // Show selected page
    document.getElementById(`${page}Page`).classList.remove('d-none');

    // Update page title
    const titles = {
        'dashboard': 'Dashboard',
        'meetings': 'Quản lý Cuộc họp',
        'departments': 'Quản lý Phòng ban',
        'rooms': 'Quản lý Phòng họp'
    };
    document.getElementById('pageTitle').textContent = titles[page];

    // Update active nav link
    document.querySelectorAll('.sidebar-nav .nav-link').forEach(link => {
        link.classList.toggle('active', link.dataset.page === page);
    });
}

function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('show');
}

function updateDateTime() {
    const now = new Date();
    const options = { 
        weekday: 'long', 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };
    document.getElementById('currentDateTime').textContent = now.toLocaleDateString('vi-VN', options);
}

// API Calls
async function apiCall(endpoint, method = 'GET', data = null) {
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json'
        }
    };

    if (data) {
        options.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || 'Có lỗi xảy ra');
        }

        return result;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// Load all data
async function loadAllData() {
    try {
        await Promise.all([
            loadDepartments(),
            loadRooms(),
            loadMeetings(),
            loadStatistics()
        ]);
    } catch (error) {
        showToast('Lỗi', 'Không thể tải dữ liệu. Vui lòng kiểm tra kết nối server.', 'error');
    }
}

// Statistics
async function loadStatistics() {
    try {
        const result = await apiCall('/meetings/statistics');
        const stats = result.data;

        document.getElementById('totalMeetings').textContent = stats.totalMeetings || 0;
        document.getElementById('scheduledMeetings').textContent = stats.scheduledMeetings || 0;
        document.getElementById('ongoingMeetings').textContent = stats.ongoingMeetings || 0;
        document.getElementById('finishedMeetings').textContent = stats.finishedMeetings || 0;
        document.getElementById('meetingsToday').textContent = stats.meetingsToday || 0;
        document.getElementById('meetingsThisWeek').textContent = stats.meetingsThisWeek || 0;
        document.getElementById('meetingsThisMonth').textContent = stats.meetingsThisMonth || 0;
        document.getElementById('meetingsThisYear').textContent = stats.meetingsThisYear || 0;
    } catch (error) {
        console.error('Error loading statistics:', error);
    }
}

// Departments
async function loadDepartments() {
    try {
        const result = await apiCall('/departments');
        departments = result.data || [];
        renderDepartmentsTable();
        updateDepartmentDropdowns();
    } catch (error) {
        console.error('Error loading departments:', error);
    }
}

function renderDepartmentsTable() {
    const tbody = document.getElementById('departmentsTable');
    
    if (departments.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center py-5">
                    <div class="empty-state">
                        <i class="bi bi-building"></i>
                        <p>Chưa có phòng ban nào</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = departments.map(dept => `
        <tr>
            <td>${dept.id}</td>
            <td><strong>${dept.name}</strong></td>
            <td>${dept.description || '-'}</td>
            <td>${formatDate(dept.createdAt)}</td>
            <td>
                <button class="btn btn-action btn-edit" onclick="editDepartment(${dept.id})" title="Sửa">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-action btn-delete" onclick="deleteDepartment(${dept.id})" title="Xóa">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function updateDepartmentDropdowns() {
    const options = departments.map(d => `<option value="${d.name}">${d.name}</option>`).join('');
    
    document.getElementById('meetingDepartment').innerHTML = `<option value="">Chọn phòng ban</option>${options}`;
    document.getElementById('filterDepartment').innerHTML = `<option value="">Tất cả phòng ban</option>${options}`;
}

function openDepartmentModal(id = null) {
    document.getElementById('departmentForm').reset();
    document.getElementById('departmentId').value = '';
    document.getElementById('departmentModalTitle').textContent = 'Thêm phòng ban';
    departmentModal.show();
}

function editDepartment(id) {
    const dept = departments.find(d => d.id === id);
    if (!dept) return;

    document.getElementById('departmentId').value = dept.id;
    document.getElementById('departmentName').value = dept.name;
    document.getElementById('departmentDescription').value = dept.description || '';
    document.getElementById('departmentModalTitle').textContent = 'Sửa phòng ban';
    departmentModal.show();
}

async function saveDepartment(e) {
    e.preventDefault();
    
    const id = document.getElementById('departmentId').value;
    const data = {
        name: document.getElementById('departmentName').value,
        description: document.getElementById('departmentDescription').value
    };

    try {
        if (id) {
            await apiCall(`/departments/${id}`, 'PUT', data);
            showToast('Thành công', 'Cập nhật phòng ban thành công');
        } else {
            await apiCall('/departments', 'POST', data);
            showToast('Thành công', 'Thêm phòng ban thành công');
        }
        departmentModal.hide();
        await loadDepartments();
    } catch (error) {
        showToast('Lỗi', error.message, 'error');
    }
}

async function deleteDepartment(id) {
    if (!confirm('Bạn có chắc chắn muốn xóa phòng ban này?')) return;

    try {
        await apiCall(`/departments/${id}`, 'DELETE');
        showToast('Thành công', 'Xóa phòng ban thành công');
        await loadDepartments();
    } catch (error) {
        showToast('Lỗi', error.message, 'error');
    }
}

// Rooms
async function loadRooms() {
    try {
        const result = await apiCall('/rooms');
        rooms = result.data || [];
        renderRoomsTable();
        updateRoomDropdowns();
    } catch (error) {
        console.error('Error loading rooms:', error);
    }
}

function renderRoomsTable() {
    const tbody = document.getElementById('roomsTable');
    
    if (rooms.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-5">
                    <div class="empty-state">
                        <i class="bi bi-door-open"></i>
                        <p>Chưa có phòng họp nào</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = rooms.map(room => `
        <tr>
            <td>${room.id}</td>
            <td><strong>${room.name}</strong></td>
            <td>${room.description || '-'}</td>
            <td>${room.capacity || '-'}</td>
            <td>${room.location || '-'}</td>
            <td>
                <button class="btn btn-action btn-edit" onclick="editRoom(${room.id})" title="Sửa">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-action btn-delete" onclick="deleteRoom(${room.id})" title="Xóa">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function updateRoomDropdowns() {
    const options = rooms.map(r => `<option value="${r.name}">${r.name}</option>`).join('');
    
    document.getElementById('meetingRoom').innerHTML = `<option value="">Chọn phòng họp</option>${options}`;
    document.getElementById('filterRoom').innerHTML = `<option value="">Tất cả phòng họp</option>${options}`;
}

function openRoomModal(id = null) {
    document.getElementById('roomForm').reset();
    document.getElementById('roomId').value = '';
    document.getElementById('roomModalTitle').textContent = 'Thêm phòng họp';
    roomModal.show();
}

function editRoom(id) {
    const room = rooms.find(r => r.id === id);
    if (!room) return;

    document.getElementById('roomId').value = room.id;
    document.getElementById('roomName').value = room.name;
    document.getElementById('roomDescription').value = room.description || '';
    document.getElementById('roomCapacity').value = room.capacity || '';
    document.getElementById('roomLocation').value = room.location || '';
    document.getElementById('roomModalTitle').textContent = 'Sửa phòng họp';
    roomModal.show();
}

async function saveRoom(e) {
    e.preventDefault();
    
    const id = document.getElementById('roomId').value;
    const data = {
        name: document.getElementById('roomName').value,
        description: document.getElementById('roomDescription').value,
        capacity: document.getElementById('roomCapacity').value ? parseInt(document.getElementById('roomCapacity').value) : null,
        location: document.getElementById('roomLocation').value
    };

    try {
        if (id) {
            await apiCall(`/rooms/${id}`, 'PUT', data);
            showToast('Thành công', 'Cập nhật phòng họp thành công');
        } else {
            await apiCall('/rooms', 'POST', data);
            showToast('Thành công', 'Thêm phòng họp thành công');
        }
        roomModal.hide();
        await loadRooms();
    } catch (error) {
        showToast('Lỗi', error.message, 'error');
    }
}

async function deleteRoom(id) {
    if (!confirm('Bạn có chắc chắn muốn xóa phòng họp này?')) return;

    try {
        await apiCall(`/rooms/${id}`, 'DELETE');
        showToast('Thành công', 'Xóa phòng họp thành công');
        await loadRooms();
    } catch (error) {
        showToast('Lỗi', error.message, 'error');
    }
}

// Meetings
async function loadMeetings() {
    try {
        const result = await apiCall('/meetings');
        meetings = result.data || [];
        renderMeetingsTable();
        renderRecentMeetings();
    } catch (error) {
        console.error('Error loading meetings:', error);
    }
}

function renderMeetingsTable(filteredMeetings = null) {
    const tbody = document.getElementById('meetingsTable');
    const data = filteredMeetings || meetings;
    
    if (data.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" class="text-center py-5">
                    <div class="empty-state">
                        <i class="bi bi-calendar-x"></i>
                        <p>Chưa có cuộc họp nào</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = data.map(meeting => `
        <tr>
            <td>${meeting.id}</td>
            <td><strong>${meeting.title}</strong></td>
            <td>${meeting.department}</td>
            <td>${meeting.room}</td>
            <td>${meeting.chairman}</td>
            <td>${meeting.secretary}</td>
            <td>${formatDateTime(meeting.startTime)}</td>
            <td>${getStatusBadge(meeting.status)}</td>
            <td>
                <button class="btn btn-action btn-join" onclick="joinMeetingRoom(${meeting.id})" title="Vào phòng họp">
                    <i class="bi bi-box-arrow-in-right"></i>
                </button>
                <button class="btn btn-action btn-status" onclick="changeStatus(${meeting.id})" title="Đổi trạng thái">
                    <i class="bi bi-arrow-repeat"></i>
                </button>
                <button class="btn btn-action btn-edit" onclick="editMeeting(${meeting.id})" title="Sửa">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-action btn-delete" onclick="deleteMeeting(${meeting.id})" title="Xóa">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function renderRecentMeetings() {
    const tbody = document.getElementById('recentMeetingsTable');
    const recentMeetings = [...meetings]
        .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))
        .slice(0, 5);
    
    if (recentMeetings.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center py-4">
                    <p class="text-muted mb-0">Chưa có cuộc họp nào</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = recentMeetings.map(meeting => `
        <tr>
            <td><strong>${meeting.title}</strong></td>
            <td>${meeting.department}</td>
            <td>${meeting.room}</td>
            <td>${formatDateTime(meeting.startTime)}</td>
            <td>
                ${getStatusBadge(meeting.status)}
                ${meeting.status !== 'FINISHED' ? 
                    `<button class="btn btn-sm btn-success ms-2" onclick="joinMeetingRoom(${meeting.id})" title="Vào phòng họp">
                        <i class="bi bi-box-arrow-in-right"></i> Vào họp
                    </button>` : ''
                }
            </td>
        </tr>
    `).join('');
}

function filterMeetings() {
    const status = document.getElementById('filterStatus').value;
    const department = document.getElementById('filterDepartment').value;
    const room = document.getElementById('filterRoom').value;
    const date = document.getElementById('filterDate').value;

    let filtered = [...meetings];

    if (status) {
        filtered = filtered.filter(m => m.status === status);
    }
    if (department) {
        filtered = filtered.filter(m => m.department === department);
    }
    if (room) {
        filtered = filtered.filter(m => m.room === room);
    }
    if (date) {
        filtered = filtered.filter(m => m.startTime.startsWith(date));
    }

    renderMeetingsTable(filtered);
}

function openMeetingModal() {
    document.getElementById('meetingForm').reset();
    document.getElementById('meetingId').value = '';
    document.getElementById('meetingModalTitle').textContent = 'Thêm cuộc họp';
    document.getElementById('statusField').style.display = 'none';
    meetingModal.show();
}

function editMeeting(id) {
    const meeting = meetings.find(m => m.id === id);
    if (!meeting) return;

    document.getElementById('meetingId').value = meeting.id;
    document.getElementById('meetingTitle').value = meeting.title;
    document.getElementById('meetingDescription').value = meeting.description || '';
    document.getElementById('meetingDepartment').value = meeting.department;
    document.getElementById('meetingRoom').value = meeting.room;
    document.getElementById('meetingChairman').value = meeting.chairman;
    document.getElementById('meetingSecretary').value = meeting.secretary;
    document.getElementById('meetingStartTime').value = formatDateTimeLocal(meeting.startTime);
    document.getElementById('meetingEndTime').value = formatDateTimeLocal(meeting.endTime);
    document.getElementById('meetingStatus').value = meeting.status;
    document.getElementById('statusField').style.display = 'block';
    document.getElementById('meetingModalTitle').textContent = 'Sửa cuộc họp';
    meetingModal.show();
}

async function saveMeeting(e) {
    e.preventDefault();
    
    const id = document.getElementById('meetingId').value;
    const data = {
        title: document.getElementById('meetingTitle').value,
        description: document.getElementById('meetingDescription').value,
        department: document.getElementById('meetingDepartment').value,
        room: document.getElementById('meetingRoom').value,
        chairman: document.getElementById('meetingChairman').value,
        secretary: document.getElementById('meetingSecretary').value,
        startTime: document.getElementById('meetingStartTime').value,
        endTime: document.getElementById('meetingEndTime').value
    };

    if (id) {
        data.status = document.getElementById('meetingStatus').value;
    }

    try {
        if (id) {
            await apiCall(`/meetings/${id}`, 'PUT', data);
            showToast('Thành công', 'Cập nhật cuộc họp thành công');
        } else {
            await apiCall('/meetings', 'POST', data);
            showToast('Thành công', 'Thêm cuộc họp thành công');
        }
        meetingModal.hide();
        await loadMeetings();
        await loadStatistics();
    } catch (error) {
        showToast('Lỗi', error.message, 'error');
    }
}

async function deleteMeeting(id) {
    if (!confirm('Bạn có chắc chắn muốn xóa cuộc họp này?')) return;

    try {
        await apiCall(`/meetings/${id}`, 'DELETE');
        showToast('Thành công', 'Xóa cuộc họp thành công');
        await loadMeetings();
        await loadStatistics();
    } catch (error) {
        showToast('Lỗi', error.message, 'error');
    }
}

async function changeStatus(id) {
    const meeting = meetings.find(m => m.id === id);
    if (!meeting) return;

    const statusOrder = ['SCHEDULED', 'ONGOING', 'FINISHED'];
    const currentIndex = statusOrder.indexOf(meeting.status);
    const nextStatus = statusOrder[(currentIndex + 1) % statusOrder.length];

    try {
        await apiCall(`/meetings/${id}/status?status=${nextStatus}`, 'PATCH');
        showToast('Thành công', `Đã chuyển trạng thái sang "${getStatusText(nextStatus)}"`);
        await loadMeetings();
        await loadStatistics();
    } catch (error) {
        showToast('Lỗi', error.message, 'error');
    }
}

// Utility Functions
function formatDate(dateString) {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('vi-VN');
}

function formatDateTime(dateString) {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatDateTimeLocal(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toISOString().slice(0, 16);
}

function getStatusBadge(status) {
    const badges = {
        'SCHEDULED': '<span class="badge badge-status badge-scheduled">Đã lên lịch</span>',
        'ONGOING': '<span class="badge badge-status badge-ongoing">Đang diễn ra</span>',
        'FINISHED': '<span class="badge badge-status badge-finished">Đã kết thúc</span>'
    };
    return badges[status] || status;
}

function getStatusText(status) {
    const texts = {
        'SCHEDULED': 'Đã lên lịch',
        'ONGOING': 'Đang diễn ra',
        'FINISHED': 'Đã kết thúc'
    };
    return texts[status] || status;
}

function showToast(title, message, type = 'success') {
    const toastEl = document.getElementById('toast');
    const toastTitle = document.getElementById('toastTitle');
    const toastBody = document.getElementById('toastBody');

    toastTitle.textContent = title;
    toastBody.textContent = message;

    // Update toast color based on type
    toastEl.classList.remove('bg-success', 'bg-danger', 'text-white');
    if (type === 'error') {
        toastEl.classList.add('bg-danger', 'text-white');
    } else {
        toastEl.classList.add('bg-success', 'text-white');
    }

    toast.show();
}

// Join Meeting Room - Speech to Text
function joinMeetingRoom(meetingId) {
    const meeting = meetings.find(m => m.id === meetingId);
    if (!meeting) {
        showToast('Lỗi', 'Không tìm thấy thông tin cuộc họp', 'error');
        return;
    }

    // Lưu thông tin cuộc họp vào localStorage để trang meeting-room.html đọc
    const meetingInfo = {
        meetingId: meeting.id,
        meetingCode: `MTG-${meeting.id}`,
        meetingName: meeting.title,
        department: meeting.department,
        room: meeting.room,
        hostName: meeting.chairman,
        secretaryName: meeting.secretary,
        startTime: formatDateTime(meeting.startTime),
        endTime: formatDateTime(meeting.endTime),
        description: meeting.description || ''
    };

    localStorage.setItem('meetingInfo', JSON.stringify(meetingInfo));

    // Chuyển sang trang phòng họp Speech-to-Text
    window.location.href = 'meeting-room.html';
}

