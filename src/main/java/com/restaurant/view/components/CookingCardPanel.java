package com.restaurant.view.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.restaurant.model.CookingStep;
import com.restaurant.model.Recipe;
import com.restaurant.service.InventoryService;
import com.restaurant.service.RecipeService;
import com.restaurant.util.KitchenOrderManager;
import com.restaurant.util.KitchenOrderManager.KitchenOrder;
import com.restaurant.util.KitchenOrderManager.OrderItem;
import com.restaurant.util.KitchenOrderManager.OrderStatus;
import com.restaurant.util.ToastNotification;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CookingCardPanel - Game-style cooking card for Kitchen Display
 * 
 * Features:
 * - Shows ingredients checklist with stock status
 * - Shows cooking steps with progress bar
 * - Auto-complete when all steps done
 * - View-only mode for Admin/Manager
 */
public class CookingCardPanel extends JPanel {
    
    // Colors
    private static final Color CARD_BG = Color.decode("#21262D");
    private static final Color SURFACE = Color.decode("#161B22");
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color TEXT_MUTED = new Color(255, 255, 255, 160);
    private static final Color COL_WAITING = Color.decode("#F59E0B");
    private static final Color COL_COOKING = Color.decode("#F97316");
    private static final Color COL_READY = Color.decode("#22C55E");
    private static final Color STATUS_URGENT = Color.decode("#EF4444");
    private static final Color INGREDIENT_OK = Color.decode("#22C55E");
    private static final Color INGREDIENT_LOW = Color.decode("#F59E0B");
    private static final Color INGREDIENT_OUT = Color.decode("#EF4444");
    
    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_TIMER = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_STEP = new Font("Segoe UI", Font.BOLD, 11);
    
    private final KitchenOrder order;
    private final OrderItem item;
    private final boolean isViewOnly;
    private final Runnable onUpdate;
    
    private List<CookingStep> cookingSteps;
    private List<Recipe> recipes;
    private Map<Integer, JCheckBox> ingredientCheckboxes = new HashMap<>();
    
    public CookingCardPanel(KitchenOrder order, OrderItem item, boolean isViewOnly, Runnable onUpdate) {
        this.order = order;
        this.item = item;
        this.isViewOnly = isViewOnly;
        this.onUpdate = onUpdate;
        
        loadData();
        initializeUI();
    }
    
    private void loadData() {
        RecipeService recipeService = RecipeService.getInstance();
        // Note: In real implementation, item would have productId
        // For now we'll use name matching or mock data
        recipes = recipeService.getRecipeByProductId(getProductId());
        cookingSteps = recipeService.getCookingSteps(getProductId());
    }
    
    private int getProductId() {
        return item.getProductId();
    }
    
    private void initializeUI() {
        long minutes = order.getMinutesElapsed();
        boolean isUrgent = minutes > 15;
        Color accentColor = item.isReady() ? COL_READY : (isUrgent ? STATUS_URGENT : COL_COOKING);
        
        setLayout(new MigLayout("wrap, insets 8, gap 4", "[grow]", ""));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        putClientProperty(FlatClientProperties.STYLE, "arc: 8; background: " + colorToHex(CARD_BG));
        setBackground(CARD_BG);
        
        // Header: Item name + quantity + timer
        add(createHeader(minutes, isUrgent), "growx");
        
        // Separator
        add(createSeparator(), "growx, h 1!, gaptop 4, gapbottom 4");
        
        // Ingredients section
        if (!recipes.isEmpty()) {
            add(createIngredientsSection(), "growx");
            add(createSeparator(), "growx, h 1!, gaptop 4, gapbottom 4");
        }
        
        // Cooking steps section
        if (!cookingSteps.isEmpty()) {
            add(createStepsSection(), "growx");
            add(createSeparator(), "growx, h 1!, gaptop 4, gapbottom 4");
        }
        
        // Action button
        if (!isViewOnly && !item.isReady()) {
            add(createActionButton(), "growx, h 28!");
        }
    }
    
    private JPanel createHeader(long minutes, boolean isUrgent) {
        JPanel header = new JPanel(new MigLayout("insets 0", "[]push[]", "[center]"));
        header.setOpaque(false);
        
        // Item name with quantity
        JLabel nameLabel = new JLabel(item.getQuantity() + "x " + item.getName());
        nameLabel.setFont(FONT_TITLE);
        nameLabel.setForeground(TEXT_WHITE);
        header.add(nameLabel);
        
        // Timer
        String timeStr = minutes < 60 ? minutes + "'" : (minutes/60) + "h" + (minutes%60) + "'";
        JLabel timerLabel = new JLabel("⏱ " + timeStr);
        timerLabel.setFont(FONT_TIMER);
        timerLabel.setForeground(isUrgent ? STATUS_URGENT : (minutes > 10 ? COL_WAITING : TEXT_MUTED));
        header.add(timerLabel);
        
        return header;
    }
    
    private JPanel createIngredientsSection() {
        JPanel panel = new JPanel(new MigLayout("wrap, insets 0, gap 2", "[grow]", ""));
        panel.setOpaque(false);
        
        JLabel title = new JLabel("🥬 Nguyên liệu");
        title.setFont(FONT_STEP);
        title.setForeground(TEXT_MUTED);
        panel.add(title, "gapbottom 2");
        
        for (Recipe recipe : recipes) {
            JPanel row = new JPanel(new MigLayout("insets 0, gap 4", "[][grow][]", ""));
            row.setOpaque(false);
            
            JCheckBox check = new JCheckBox();
            check.setOpaque(false);
            check.setFocusPainted(false);
            check.setEnabled(!isViewOnly);
            ingredientCheckboxes.put(recipe.getIngredientId(), check);
            row.add(check);
            
            String display = String.format("%.0f%s %s", 
                recipe.getQuantityUsed() * item.getQuantity(),
                recipe.getIngredientUnit(), 
                recipe.getIngredientName());
            JLabel label = new JLabel(display);
            label.setFont(FONT_BODY);
            label.setForeground(TEXT_WHITE);
            row.add(label, "growx");
            
            // Stock indicator
            double needed = recipe.getQuantityUsed() * item.getQuantity();
            Color stockColor = recipe.getIngredientStock() >= needed ? INGREDIENT_OK :
                               recipe.getIngredientStock() >= needed * 0.5 ? INGREDIENT_LOW : INGREDIENT_OUT;
            JLabel stockDot = new JLabel("●");
            stockDot.setForeground(stockColor);
            stockDot.setToolTipText(String.format("Kho: %.0f%s", recipe.getIngredientStock(), recipe.getIngredientUnit()));
            row.add(stockDot);
            
            panel.add(row, "growx");
        }
        
        return panel;
    }
    
    private JPanel createStepsSection() {
        JPanel panel = new JPanel(new MigLayout("wrap, insets 0, gap 4", "[grow]", ""));
        panel.setOpaque(false);
        
        JLabel title = new JLabel("📋 Các bước");
        title.setFont(FONT_STEP);
        title.setForeground(TEXT_MUTED);
        panel.add(title, "gapbottom 2");
        
        // Progress bar
        int totalSteps = cookingSteps.size();
        int currentStep = item.getCurrentStep();
        int completedSteps = Math.min(currentStep, totalSteps);
        int progress = totalSteps > 0 ? (completedSteps * 100 / totalSteps) : 0;
        
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(progress);
        progressBar.setStringPainted(true);
        progressBar.setString(completedSteps + "/" + totalSteps + " (" + progress + "%)");
        progressBar.setForeground(COL_COOKING);
        progressBar.setBackground(SURFACE);
        progressBar.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        panel.add(progressBar, "growx, h 20!");
        
        // Step indicators
        JPanel stepsRow = new JPanel(new MigLayout("insets 0, gap 2", "", ""));
        stepsRow.setOpaque(false);
        
        for (int i = 0; i < cookingSteps.size(); i++) {
            CookingStep step = cookingSteps.get(i);
            boolean isDone = i < item.getCurrentStep();
            boolean isCurrent = i == item.getCurrentStep();
            
            Color stepColor = isDone ? COL_READY : (isCurrent ? COL_COOKING : TEXT_MUTED);
            String stepText = step.getIcon() + " " + step.getTitle();
            
            JLabel stepLabel = new JLabel(stepText);
            stepLabel.setFont(new Font("Segoe UI", isCurrent ? Font.BOLD : Font.PLAIN, 10));
            stepLabel.setForeground(stepColor);
            stepLabel.setToolTipText(step.getDescription());
            
            stepsRow.add(stepLabel);
            
            if (i < cookingSteps.size() - 1) {
                JLabel arrow = new JLabel(" → ");
                arrow.setForeground(TEXT_MUTED);
                stepsRow.add(arrow);
            }
        }
        
        panel.add(stepsRow, "growx");
        
        return panel;
    }
    
    private JButton createActionButton() {
        String buttonText;
        Color buttonColor;
        int currentStep = item.getCurrentStep();
        int totalSteps = cookingSteps.size();
        
        if (currentStep == 0) {
            buttonText = "🔥 Bắt đầu nấu";
            buttonColor = COL_COOKING;
        } else if (currentStep < totalSteps) {
            CookingStep nextStep = cookingSteps.get(currentStep);
            buttonText = "✓ Xong: " + nextStep.getTitle();
            buttonColor = COL_COOKING;
        } else {
            buttonText = "✅ Hoàn thành món";
            buttonColor = COL_READY;
        }
        
        JButton btn = new JButton(buttonText);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(TEXT_WHITE);
        btn.setBackground(buttonColor);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        
        btn.addActionListener(e -> handleAction());
        
        return btn;
    }
    
    private void handleAction() {
        int currentStep = item.getCurrentStep();
        int totalSteps = cookingSteps.size();
        
        if (currentStep == 0) {
            // Start cooking
            item.setCurrentStep(1);
            order.setStatus(OrderStatus.PREPARING);
            ToastNotification.info(SwingUtilities.getWindowAncestor(this), 
                "Bắt đầu nấu: " + item.getName());
        } else if (currentStep < totalSteps) {
            // Complete current step - advance to next
            item.advanceStep();
        } else {
            // Complete the item
            item.setReady(true);
            
            // Deduct ingredients if not training mode
            InventoryService.getInstance().deductIngredients(
                getProductId(), item.getQuantity(), order.getId());
            
            ToastNotification.success(SwingUtilities.getWindowAncestor(this), 
                "Hoàn thành: " + item.getName());
            Toolkit.getDefaultToolkit().beep();
        }
        
        if (onUpdate != null) {
            onUpdate.run();
        }
    }
    
    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 30));
        return sep;
    }
    
    private String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
