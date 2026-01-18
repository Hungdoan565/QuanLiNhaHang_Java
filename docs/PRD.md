# 📋 Product Requirements Document (PRD)
## Hệ thống Quản lý Nhà hàng - Restaurant Management System

---

## 1. Tổng quan sản phẩm

### 1.1 Tên sản phẩm
**RestaurantPOS** - Hệ thống Quản lý Nhà hàng Desktop

### 1.2 Mục tiêu
Xây dựng ứng dụng Desktop quản lý toàn diện hoạt động nhà hàng, tối ưu quy trình từ order → bếp → thanh toán, đảm bảo kiểm soát thất thoát và báo cáo doanh thu chính xác.

### 1.3 Tech Stack
| Thành phần | Công nghệ |
|------------|----------|
| **Frontend** | Java Swing + FlatLaf |
| **Backend** | Java 17+ (MVC + DAO Pattern) |
| **Database** | MySQL 8.0 |
| **Connection Pool** | HikariCP |
| **Build Tool** | Maven |
| **Logging** | Log4j2 / SLF4J |
| **Reporting** | JasperReports + JFreeChart |
| **Network** | Local LAN (Offline-capable) |

### 1.4 Target Users
- **Nhà hàng quy mô nhỏ - vừa** (20-100 bàn)
- **Quán ăn, café, bar** có nhu cầu quản lý chuyên nghiệp
- **Chuỗi F&B** muốn triển khai hệ thống POS nội bộ

---

## 2. Phân quyền người dùng (User Roles)

| Role | Mô tả | Quyền hạn chính |
|------|-------|-----------------|
| **ADMIN** | Quản lý cấp cao | Toàn quyền hệ thống, báo cáo, cấu hình |
| **CASHIER** | Thu ngân | Thanh toán, in hóa đơn, chốt ca |
| **WAITER** | Phục vụ | Order món, quản lý bàn, gộp/tách bàn |
| **CHEF** | Bếp/Bar | Xem order, đổi trạng thái món |

### 2.1 Ma trận phân quyền chi tiết

| Chức năng | ADMIN | CASHIER | WAITER | CHEF |
|-----------|:-----:|:-------:|:------:|:----:|
| Dashboard tổng quan | ✅ | ❌ | ❌ | ❌ |
| Đổi trạng thái bàn | ✅ | ✅ | ✅ | ❌ |
| Thêm/Xóa/Sửa vị trí bàn | ✅ | ❌ | ❌ | ❌ |
| Order món | ✅ | ✅ | ✅ | ❌ |
| Thanh toán | ✅ | ✅ | ❌ | ❌ |
| Mở/Đóng ca làm việc | ✅ | ✅ | ❌ | ❌ |
| Hủy món (đã in bếp) | ✅ | ❌ | ❌ | ❌ |
| Hủy hóa đơn | ✅ | ❌ | ❌ | ❌ |
| Xem Kitchen Display | ✅ | ❌ | ❌ | ✅ |
| Quản lý thực đơn | ✅ | ❌ | ❌ | ❌ |
| Quản lý kho | ✅ | ❌ | ❌ | ❌ |
| Quản lý nhân sự | ✅ | ❌ | ❌ | ❌ |
| Xem báo cáo | ✅ | ❌ | ❌ | ❌ |
| Cài đặt hệ thống | ✅ | ❌ | ❌ | ❌ |

> [!NOTE]
> **WAITER** chỉ có quyền đổi trạng thái bàn (Trống → Có khách), không có quyền thêm/xóa/sửa vị trí bàn trong sơ đồ.

---

## 3. Modules chức năng

### 3.1 Module POS - Bán hàng tại bàn ⭐ (Core)

#### 3.1.1 Sơ đồ bàn (Table Map)
**User Stories:**
- [ ] Hiển thị sơ đồ bàn trực quan với màu sắc trạng thái
- [ ] Mở bàn mới (khách vào) + **Nhập số lượng khách (Guest Count)**
- [ ] Đặt bàn trước (Reservation)
- [ ] Chuyển bàn (Move Table)
- [ ] Gộp bàn (Merge Tables)
- [ ] Tách bàn (Split Table)

**Trạng thái bàn:**
| Trạng thái | Màu | Mô tả |
|------------|-----|-------|
| AVAILABLE | 🟢 Xanh lá | Bàn trống |
| OCCUPIED | 🔴 Đỏ | Đang có khách |
| RESERVED | 🟡 Vàng | Đã đặt trước |
| CLEANING | 🟠 Cam | Đang dọn dẹp |

> [!TIP]
> **Guest Count** giúp tính "Doanh thu trung bình/đầu người" (Average Check) - KPI quan trọng của nhà hàng.

#### 3.1.2 Order món (Ordering)
**User Stories:**
- [ ] Chọn món theo danh mục (Category)
- [ ] Tìm kiếm món nhanh (Search)
- [ ] Thêm ghi chú món (Note: ít cay, không hành...)
- [ ] Chọn Modifier/Topping (Size, Topping có tính tiền)
- [ ] Cập nhật số lượng (+/-)
- [ ] Xóa món (trước khi in bếp)
- [ ] In order xuống bếp (Kitchen Ticket)

**Business Rules:**
> [!IMPORTANT]
> - Món đã in xuống bếp → Chỉ ADMIN mới được hủy
> - Mỗi lần hủy phải ghi lý do và log vào Audit

#### 3.1.3 Printer Routing (Điều hướng máy in) 🆕
| Danh mục món | Máy in đích |
|-------------|-------------|
| Đồ uống, Bia, Cocktail | Máy in **Bar** |
| Món chính, Khai vị | Máy in **Bếp** |
| Tráng miệng | Máy in **Bếp** hoặc **Bar** (cấu hình) |

> [!NOTE]
> Mỗi danh mục món cần gắn với 1 máy in. Khi order, hệ thống tự động gửi ticket đến đúng máy.

---

### 3.2 Module Kitchen Display System (KDS)

**User Stories:**
- [ ] Hiển thị danh sách order theo thời gian (FIFO)
- [ ] Đổi trạng thái món: `PENDING → COOKING → READY`
- [ ] Cảnh báo màu theo thời gian chờ
- [ ] Thông báo cho Waiter khi món xong

**Logic Nhóm món (Grouping):** 🆕
> [!IMPORTANT]
> - Mặc định: **KHÔNG gộp tự động** → Đảm bảo FIFO, mỗi bàn 1 ticket riêng
> - Bếp có thể **chủ động gộp** nếu thấy nhiều bàn gọi món giống nhau
> - Hệ thống chỉ **gợi ý** (highlight món trùng) chứ không tự gộp

**UI Requirements:**
- Dark Mode (nền đen, chữ trắng)
- Font size lớn (18-24pt)
- Touch-friendly buttons

---

### 3.3 Module Billing - Thanh toán

**User Stories:**
- [ ] Xem bill tạm tính (Preview)
- [ ] Áp dụng giảm giá (Discount %, Discount VNĐ)
- [ ] Áp dụng Voucher Code
- [ ] Tính phụ phí (VAT, Service Charge)
- [ ] Chọn hình thức thanh toán (Tiền mặt, Chuyển khoản, Thẻ)
- [ ] Tách hóa đơn (Split Bill)
- [ ] In hóa đơn (PDF / Máy in nhiệt)
- [ ] Đóng bàn sau thanh toán
- [ ] **Mở két tiền tự động** (Open Cash Drawer) khi thanh toán xong

**Hình thức thanh toán:**
| Loại | Yêu cầu đặc biệt |
|------|------------------|
| **Tiền mặt** | Nhập số tiền khách đưa, tính tiền thối |
| **Chuyển khoản** | Hiển thị QR Code, xác nhận đã nhận tiền |
| **Thẻ** | Tích hợp máy POS (nếu có) |

#### 3.3.1 Quản lý Ca làm việc (Shift Management) 🆕
**User Stories:**
- [ ] Mở ca (Open Shift): Nhập số tiền đầu ca (Floating money)
- [ ] Đóng ca (Close Shift): Nhập tổng tiền mặt đếm được thực tế
- [ ] Báo cáo kết ca (Z-Report): So sánh tiền hệ thống vs. Tiền thực tế → Ra số lệch (Variance)

> [!CAUTION]
> Không có Shift Management = Không thể kiểm soát thất thoát tiền mặt!

---

### 3.4 Module Inventory - Quản lý Kho

**User Stories:**
- [ ] Quản lý danh mục nguyên liệu
- [ ] Nhập kho (Stock In)
- [ ] Xuất kho thủ công (Stock Out)
- [ ] Trừ kho tự động theo định lượng (Recipe)
- [ ] Cảnh báo tồn kho thấp
- [ ] Kiểm kê kho (Stock Taking)
- [ ] Báo cáo hao hụt (Loss Report)

**Định lượng (Recipe):**
```
Ví dụ: 1 ly Cà phê sữa
├── Cà phê hạt: 20g
├── Sữa đặc: 30ml
└── Đá: 100g

→ Khi bán 1 ly, kho tự động trừ nguyên liệu
```

**Negative Stock Policy (Kho âm):** 🆕
> [!WARNING]
> - Nếu kho báo hết nguyên liệu → **Vẫn cho bán** (vì kho phần mềm có thể sai so với thực tế)
> - Hệ thống sẽ **cảnh báo đỏ** trên màn hình POS
> - Ghi nhận kho âm để kiểm kê sau

---

### 3.5 Module Menu - Quản lý Thực đơn

**User Stories:**
- [ ] CRUD Danh mục món (Category)
- [ ] CRUD Món ăn (Product)
- [ ] Upload hình ảnh món
- [ ] Đặt giá bán, giá vốn
- [ ] Gắn định lượng nguyên liệu
- [ ] Thiết lập Modifier Groups (Size, Topping)
- [ ] Ẩn/Hiện món (Available/Unavailable)

---

### 3.6 Module HRM - Quản lý Nhân sự

**User Stories:**
- [ ] CRUD Nhân viên
- [ ] Gán Role/Phân quyền
- [ ] Quản lý tài khoản đăng nhập
- [ ] Đổi mật khẩu
- [ ] Chấm công (Check-in/Check-out)
- [ ] Xem lịch sử đăng nhập

---

### 3.7 Module Reporting - Báo cáo

**User Stories:**
- [ ] Doanh thu theo ngày/tuần/tháng/năm
- [ ] Top món bán chạy (Best Sellers)
- [ ] Báo cáo theo nhân viên
- [ ] Báo cáo tồn kho
- [ ] Báo cáo lãi/lỗ (nếu có giá vốn)
- [ ] Xuất báo cáo Excel/PDF

**Charts:**
- Bar Chart: Doanh thu theo ngày
- Pie Chart: Tỷ lệ món theo danh mục
- Line Chart: Xu hướng doanh thu

---

## 4. Non-Functional Requirements

### 4.1 Performance
- Khởi động app: < 3 giây
- Tìm kiếm món: < 500ms
- In hóa đơn: < 2 giây

### 4.2 Security
- Password hash: BCrypt
- Session timeout: 8 giờ (1 ca làm việc)
- Audit Log cho tất cả thao tác nhạy cảm

### 4.3 Reliability
- Hoạt động offline (LAN-based)
- Auto-save order mỗi 30 giây
- Backup database tự động

### 4.4 Usability
- Hỗ trợ phím tắt (Hotkeys)
- Touch-friendly UI
- Dark Mode cho Kitchen Display

---

## 5. Milestones

| Phase | Thời gian | Deliverables |
|-------|-----------|--------------|
| **Phase 1** | Tuần 1-2 | Documentation, Database Design, Project Setup |
| **Phase 2** | Tuần 3-4 | Auth + POS Module (Table + Order) |
| **Phase 3** | Tuần 5-6 | Kitchen Display + Billing |
| **Phase 4** | Tuần 7-8 | Inventory + Menu Management |
| **Phase 5** | Tuần 9-10 | HRM + Reporting |
| **Phase 6** | Tuần 11-12 | Testing + Polish + Deployment |

---

## 6. Success Metrics

| Metric | Target |
|--------|--------|
| Thời gian order 1 món | < 5 giây |
| Thời gian thanh toán | < 30 giây |
| Uptime hệ thống | 99.9% |
| Bug Rate sau release | < 5% |
| User Satisfaction | ≥ 4/5 ⭐ |

---

## 7. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Database corruption | High | Backup tự động hàng ngày |
| Network LAN chập chờn | Medium | Retry logic + Queue offline |
| Nhân viên thao tác sai | Medium | Validation + Confirmation dialogs |
| Thất thoát tiền | High | Audit Log + Phân quyền chặt |

---

*Document Version: 1.1*  
*Last Updated: 2026-01-12*  
*Changelog: Thêm HikariCP, Log4j2, Guest Count, Printer Routing, Shift Management, Negative Stock Policy*
