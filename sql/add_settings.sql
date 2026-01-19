-- Add missing settings keys to database
-- Run this once to ensure all settings exist

INSERT IGNORE INTO settings (setting_key, setting_value, setting_type, description) VALUES
('restaurant_logo', '', 'STRING', 'Path to restaurant logo image'),
('vat_enabled', 'true', 'BOOLEAN', 'Enable VAT calculation'),
('service_charge_enabled', 'true', 'BOOLEAN', 'Enable service charge'),
('print_customer_receipt', 'true', 'BOOLEAN', 'Print receipt for customer'),
('printer_kitchen', 'Kitchen_Printer', 'STRING', 'Kitchen printer name'),
('printer_bar', 'Bar_Printer', 'STRING', 'Bar printer name'),
('printer_cashier', 'Cashier_Printer', 'STRING', 'Cashier printer name'),
('display_theme', 'light', 'STRING', 'UI theme: light, dark, system'),
('display_font_size', '14', 'NUMBER', 'Default font size'),
('display_primary_color', '#28a745', 'STRING', 'Primary accent color'),
('display_kitchen_columns', '4', 'NUMBER', 'Kitchen display columns');

-- Update existing values if needed
UPDATE settings SET setting_value = 'true' WHERE setting_key = 'vat_enabled' AND setting_value IS NULL;
UPDATE settings SET setting_value = 'true' WHERE setting_key = 'service_charge_enabled' AND setting_value IS NULL;
