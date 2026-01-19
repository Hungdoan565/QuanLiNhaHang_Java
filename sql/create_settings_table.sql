-- 1. TẠO BẢNG SETTINGS TRƯỚC
CREATE TABLE IF NOT EXISTS settings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    setting_type ENUM('STRING', 'NUMBER', 'BOOLEAN', 'JSON') DEFAULT 'STRING',
    description VARCHAR(255),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by INT,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_settings_key ON settings(setting_key);

-- 2. INSERT DỮ LIỆU MẶC ĐỊNH
INSERT INTO settings (setting_key, setting_value, setting_type, description) VALUES
('restaurant_name', 'RestaurantPOS', 'STRING', 'Tên nhà hàng'),
('restaurant_address', '123 Nguyễn Huệ, Quận 1, TP.HCM', 'STRING', 'Địa chỉ nhà hàng'),
('restaurant_phone', '028 1234 5678', 'STRING', 'Số điện thoại'),
('restaurant_logo', '', 'STRING', 'Path to restaurant logo'),
('vat_percent', '10', 'NUMBER', 'Phần trăm thuế VAT'),
('vat_enabled', 'true', 'BOOLEAN', 'Bật tính VAT'),
('service_charge_percent', '5', 'NUMBER', 'Phần trăm phí dịch vụ'),
('service_charge_enabled', 'true', 'BOOLEAN', 'Bật phí dịch vụ'),
('receipt_footer', 'Cảm ơn quý khách!\nHẹn gặp lại!', 'STRING', 'Ghi chú cuối hóa đơn'),
('kitchen_auto_print', 'true', 'BOOLEAN', 'Tự động in bếp'),
('print_customer_receipt', 'true', 'BOOLEAN', 'In hóa đơn khách'),
('printer_kitchen', 'Kitchen_Printer', 'STRING', 'Máy in bếp'),
('printer_bar', 'Bar_Printer', 'STRING', 'Máy in bar'),
('printer_cashier', 'Cashier_Printer', 'STRING', 'Máy in thu ngân'),
('display_theme', 'light', 'STRING', 'Giao diện: light/dark'),
('display_font_size', '14', 'NUMBER', 'Cỡ chữ mặc định'),
('display_primary_color', '#E85A4F', 'STRING', 'Màu chủ đạo'),
('display_kitchen_columns', '4', 'NUMBER', 'Số cột màn hình bếp');
