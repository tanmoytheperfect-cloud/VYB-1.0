package com.example.domain.tools

enum class ToolCategory(val title: String) {
    INFORMATION("Information & Research"),
    PERSONAL("Personal & Organization"),
    PRODUCTIVITY("Productivity & Communication"),
    COMMERCE("Commerce & Shopping"),
    FINANCE("Finance & Wealth"),
    RIDE("Ride & Transit"),
    DEVICE("Device & Sensors")
}

data class VybTool(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolCategory,
    val requiredPermissions: List<String> = emptyList(),
    val riskLevel: Int = 1, // 0 = Safe/Read-only, 1 = Local Write, 2 = Network/Message, 3 = Financial
    val requiresConfirmation: Boolean = riskLevel >= 2
)

object ToolRegistry {
    val allTools: List<VybTool> = listOf(
        VybTool("web_search", "Web & Intelligence Search", "Live web search and research engine", ToolCategory.INFORMATION, riskLevel = 0),
        VybTool("weather_service", "Hyperlocal Weather Engine", "Weather and precipitation forecasts for Kolkata/India", ToolCategory.INFORMATION, riskLevel = 0),
        VybTool("map_navigation", "Maps & Routing", "Calculates distances, transit times and route optimization", ToolCategory.INFORMATION, riskLevel = 0),
        VybTool("calendar_sync", "Calendar & Schedule", "Schedules, reads and modifies calendar events", ToolCategory.PERSONAL, requiredPermissions = listOf("android.permission.READ_CALENDAR"), riskLevel = 1),
        VybTool("task_manager", "Task & Priority Protocol", "Creates and organizes structured task workflows", ToolCategory.PERSONAL, riskLevel = 1),
        VybTool("reminder_scheduler", "Smart Reminders", "Alerts user for routines, school pick-ups and deadlines", ToolCategory.PERSONAL, riskLevel = 1),
        VybTool("email_dispatcher", "Executive Email Draft & Send", "Drafts and dispatches emails with user approval", ToolCategory.PRODUCTIVITY, riskLevel = 2, requiresConfirmation = true),
        VybTool("document_processor", "Document & OCR Extractor", "Extracts structured data from bills, receipts and tickets", ToolCategory.PRODUCTIVITY, riskLevel = 1),
        VybTool("price_comparison", "Indian Commerce & Price Radar", "Compares Amazon, Flipkart and local store prices in ₹ INR", ToolCategory.COMMERCE, riskLevel = 1),
        VybTool("cart_preparer", "Cart Preparation", "Prepares shopping orders and lists", ToolCategory.COMMERCE, riskLevel = 2, requiresConfirmation = true),
        VybTool("expense_tracker", "Expense & SIP Tracker", "Records spending and categorizes monthly cashflow in ₹", ToolCategory.FINANCE, riskLevel = 1),
        VybTool("affordability_checker", "Affordability & Budget Impact", "Analyzes if a purchase fits within monthly savings target", ToolCategory.FINANCE, riskLevel = 0),
        VybTool("ride_comparator", "Multi-App Cab Aggregator", "Compares live fares across Uber, Ola, Rapido & Yatri Sathi", ToolCategory.RIDE, riskLevel = 1),
        VybTool("ride_booker", "Cab Booking Dispatcher", "Dispatches booking to installed ride apps or web portal", ToolCategory.RIDE, riskLevel = 3, requiresConfirmation = true),
        VybTool("location_sensor", "GPS Location Sensor", "Accesses current device coordinates with explicit permission", ToolCategory.DEVICE, requiredPermissions = listOf("android.permission.ACCESS_FINE_LOCATION"), riskLevel = 1)
    )

    fun findTool(id: String): VybTool? = allTools.find { it.id == id }
    fun getToolsForCategory(category: ToolCategory): List<VybTool> = allTools.filter { it.category == category }
}
