package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FamilyMemberEntity
import com.example.ui.components.VybGlowCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

data class TempFamilyMember(
    val name: String,
    val relation: String,
    val age: String = "",
    val occupationOrSchool: String = "",
    val routine: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutiveOnboardingScreen(
    viewModel: VybViewModel,
    onOnboardingComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }

    // Step 1: Owner Profile
    var ownerName by remember { mutableStateOf("") }
    var ownerTitle by remember { mutableStateOf("") }
    var ownerEmail by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var homeAddress by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var officeAddress by remember { mutableStateOf("") }

    // Step 2: Family Setup
    var familyList by remember {
        mutableStateOf(emptyList<TempFamilyMember>())
    }
    var showAddFamilyDialog by remember { mutableStateOf(false) }

    // Step 3: Financial Baseline & Currency
    var monthlyIncome by remember { mutableStateOf("") }
    var fixedCommitments by remember { mutableStateOf("") }
    var monthlySavingsTarget by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("₹") }

    // Step 4: Device Automation & Consent
    var consentGiven by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VYB EXECUTIVE INDUCTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Personal Assistant Setup",
                            style = MaterialTheme.typography.titleLarge,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = "STEP $currentStep OF 4",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Step Bar Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= currentStep) CyanNeon else Slate800)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body Content per Step
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "onboarding_step"
                ) { step ->
                    when (step) {
                        1 -> Step1OwnerProfile(
                            name = ownerName, onNameChange = { ownerName = it; ownerTitle = "Sir $it" },
                            title = ownerTitle, onTitleChange = { ownerTitle = it },
                            email = ownerEmail, onEmailChange = { ownerEmail = it },
                            phone = ownerPhone, onPhoneChange = { ownerPhone = it },
                            city = city, onCityChange = { city = it },
                            home = homeAddress, onHomeChange = { homeAddress = it },
                            company = companyName, onCompanyChange = { companyName = it },
                            job = jobTitle, onJobChange = { jobTitle = it },
                            office = officeAddress, onOfficeChange = { officeAddress = it }
                        )
                        2 -> Step2FamilyStructure(
                            familyList = familyList,
                            onRemove = { member -> familyList = familyList.filter { it != member } },
                            onAddClick = { showAddFamilyDialog = true }
                        )
                        3 -> Step3FinanceSchedule(
                            income = monthlyIncome, onIncomeChange = { monthlyIncome = it },
                            fixed = fixedCommitments, onFixedChange = { fixedCommitments = it },
                            savings = monthlySavingsTarget, onSavingsChange = { monthlySavingsTarget = it },
                            currency = selectedCurrency, onCurrencyChange = { selectedCurrency = it }
                        )
                        4 -> Step4AppExecutionConsent(
                            consent = consentGiven,
                            onConsentChange = { consentGiven = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 4) {
                            currentStep++
                        } else {
                            // Finish Onboarding & Save Everything
                            val inc = monthlyIncome.toDoubleOrNull() ?: 0.0
                            val fix = fixedCommitments.toDoubleOrNull() ?: 0.0
                            val sav = monthlySavingsTarget.toDoubleOrNull() ?: 0.0

                            viewModel.saveComprehensiveProfile(
                                name = ownerName,
                                title = ownerTitle,
                                email = ownerEmail,
                                phone = ownerPhone,
                                homeAddress = homeAddress,
                                officeAddress = officeAddress,
                                jobTitle = jobTitle,
                                company = companyName,
                                city = city,
                                monthlyIncome = inc,
                                fixedCommitments = fix,
                                savingsTarget = sav,
                                currency = selectedCurrency
                            )

                            // Save family members to Room
                            familyList.forEach { f ->
                                viewModel.addFamilyMember(
                                    name = f.name,
                                    relation = f.relation,
                                    age = f.age.toIntOrNull() ?: 0,
                                    occupationOrSchool = f.occupationOrSchool,
                                    notes = f.routine,
                                    routineSummary = f.routine
                                )
                            }

                            viewModel.completeOnboarding()
                            onOnboardingComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (currentStep < 4) "Continue" else "Initialize Personal Assistant",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (currentStep < 4) Icons.Default.ArrowForward else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Add Family Dialog
    if (showAddFamilyDialog) {
        var newName by remember { mutableStateOf("") }
        var newRelation by remember { mutableStateOf("Son") }
        var newAge by remember { mutableStateOf("") }
        var newSchoolJob by remember { mutableStateOf("") }
        var newRoutine by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFamilyDialog = false },
            containerColor = Slate900,
            title = {
                Text("Add Family Member", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name", color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRelation,
                        onValueChange = { newRelation = it },
                        label = { Text("Relation (Spouse, Son, Daughter, Parent, etc.)", color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAge,
                        onValueChange = { newAge = it },
                        label = { Text("Age", color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSchoolJob,
                        onValueChange = { newSchoolJob = it },
                        label = { Text("School / College / Occupation", color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRoutine,
                        onValueChange = { newRoutine = it },
                        label = { Text("Daily Commute / Routine Summary", color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            familyList = familyList + TempFamilyMember(
                                name = newName.trim(),
                                relation = newRelation.trim(),
                                age = newAge.trim(),
                                occupationOrSchool = newSchoolJob.trim(),
                                routine = newRoutine.trim()
                            )
                            showAddFamilyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                ) {
                    Text("Add Member", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFamilyDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

@Composable
private fun Step1OwnerProfile(
    name: String, onNameChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    home: String, onHomeChange: (String) -> Unit,
    company: String, onCompanyChange: (String) -> Unit,
    job: String, onJobChange: (String) -> Unit,
    office: String, onOfficeChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Welcome to your Personal AI Assistant. To serve you accurately, I first need to learn who you are, where you live, and what you do.",
                color = Slate300,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Your Full Name", color = Slate400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate200,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder
                ),
                modifier = Modifier.fillMaxWidth().testTag("input_owner_name")
            )
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Preferred Title / Salutation (e.g. Sir Tanmoy, Chief, Boss)", color = Slate400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate200,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("City", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone Number", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = home,
                onValueChange = onHomeChange,
                label = { Text("Home Address (Used for navigation & cab pickup)", color = Slate400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate200,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = company,
                    onValueChange = onCompanyChange,
                    label = { Text("Company / Business", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = job,
                    onValueChange = onJobChange,
                    label = { Text("Job Title / Role", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = office,
                onValueChange = onOfficeChange,
                label = { Text("Office / Workplace Address", color = Slate400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate200,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Step2FamilyStructure(
    familyList: List<TempFamilyMember>,
    onRemove: (TempFamilyMember) -> Unit,
    onAddClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "A true personal assistant must know and care for your family members just like you do. Add your spouse, children, or dependents so I can coordinate school commutes, appointments, and family plans.",
                color = Slate300,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        items(familyList) { member ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(member.name, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanNeon.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = member.relation + if (member.age.isNotBlank()) " (${member.age} yrs)" else "",
                                    color = CyanNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (member.occupationOrSchool.isNotBlank()) {
                            Text(member.occupationOrSchool, color = Slate300, fontSize = 12.sp)
                        }
                        if (member.routine.isNotBlank()) {
                            Text("🕒 ${member.routine}", color = AmberNeon, fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = { onRemove(member) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CrimsonNeon, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onAddClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Another Family Member", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Step3FinanceSchedule(
    income: String, onIncomeChange: (String) -> Unit,
    fixed: String, onFixedChange: (String) -> Unit,
    savings: String, onSavingsChange: (String) -> Unit,
    currency: String, onCurrencyChange: (String) -> Unit
) {
    val incVal = income.toDoubleOrNull() ?: 0.0
    val fixVal = fixed.toDoubleOrNull() ?: 0.0
    val savVal = savings.toDoubleOrNull() ?: 0.0
    val discretionary = (incVal - fixVal - savVal).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Enter your monthly financial baseline. This allows me to calculate real affordability, protect your savings goals, and ensure every purchase is 100% safe.",
                color = Slate300,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("₹ (INR)", "$ (USD)", "€ (EUR)", "£ (GBP)").forEach { curStr ->
                    val sym = curStr.take(1)
                    val isSel = currency == sym
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) CyanNeon.copy(alpha = 0.2f) else Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) CyanNeon else CardBorder),
                        modifier = Modifier.weight(1f).clickable { onCurrencyChange(sym) }
                    ) {
                        Text(
                            text = curStr,
                            color = if (isSel) CyanNeon else Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = income,
                onValueChange = onIncomeChange,
                label = { Text("Monthly Net Income ($currency)", color = Slate400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate200,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = fixed,
                onValueChange = onFixedChange,
                label = { Text("Fixed Commitments (Rent, EMIs, Bills) ($currency)", color = Slate400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate200,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = savings,
                onValueChange = onSavingsChange,
                label = { Text("Monthly Target Savings / Investment ($currency)", color = Slate400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate200,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            VybGlowCard(
                borderColor = EmeraldNeon.copy(alpha = 0.5f),
                glowColor = EmeraldNeon.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACTIVE DISCRETIONARY BUFFER", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$currency ${discretionary.toInt()}", color = EmeraldNeon, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Text("Calculated automatically", color = Slate400, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun Step4AppExecutionConsent(
    consent: Boolean,
    onConsentChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "VYB is an AI Personal Assistant that can access your mobile device apps to execute tasks on your behalf.",
                color = Slate100,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        val appCapabilities = listOf(
            Triple("🚕 Cabs & Transport", "Uber, Ola, Rapido, Yatri Sathi", "Dispatches school and commute rides with fare comparison"),
            Triple("💬 WhatsApp & Messaging", "WhatsApp, Messages / SMS", "Drafts messages to suppliers, colleagues, and family"),
            Triple("🗺️ Maps & Navigation", "Google Maps", "Initiates turn-by-turn routes to home, office, and destinations"),
            Triple("📞 Phone & Dialer", "Android Dialer", "Prepares calls to contacts and suppliers upon your tap"),
            Triple("🛍️ Shopping & Quick Commerce", "Amazon, Flipkart, Blinkit", "Searches products and launches shopping carts"),
            Triple("✉️ Email & Calendar", "Gmail, Google Calendar", "Drafts emails and schedules calendar reminders")
        )

        items(appCapabilities) { (title, apps, desc) ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(apps, color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(desc, color = Slate400, fontSize = 11.sp)
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().clickable { onConsentChange(!consent) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(
                        checked = consent,
                        onCheckedChange = onConsentChange,
                        colors = CheckboxDefaults.colors(checkedColor = CyanNeon, checkmarkColor = Slate950)
                    )
                    Column {
                        Text(
                            text = "I authorize VYB to prepare app actions on my phone.",
                            color = Slate100,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "VYB will ALWAYS prompt you for approval before opening any external app or placing orders.",
                            color = AmberNeon,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
