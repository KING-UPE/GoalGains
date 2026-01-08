# 🍎 GoalGains – Smart Calorie & Macro Tracker ✨

**GoalGains** is a modern, high-performance **Android application** designed to simplify nutrition management.  
It empowers users to achieve fitness goals through real-time calorie tracking, automated macro calculation, and seamless food logging.

---

## 🎯 Core Features

GoalGains provides a comprehensive suite of tools for health-conscious users:

---

### 📊 Intelligent Dashboard
The central hub of your fitness journey:

- **Dynamic Progress Rings:** Smooth animated calorie intake tracking  
- **Macro Breakdown:** Real-time tracking of Carbs, Protein, and Fats  
- **Daily Motivation:** Health quotes fetched via **ZenQuotes API** on app launch  
- **Activity Streak:** Automated streak tracking to reinforce consistency  

---

### 🔍 Advanced Food Search (Hybrid)
Instant nutritional lookup powered by two data sources:

- **Local Database:** Curated healthy foods stored in **Firebase Firestore**  
- **Online API (OpenFoodFacts):** Toggle online search to fetch branded products via REST APIs  
- **Smart Filtering:** Search by name, category, or “Liked” foods  

---

### 📝 Seamless Meal Logging
Effortless food intake recording:

- **Cart System:** Add multiple foods before logging them together  
- **Meal Templates:** Save frequent meals (e.g., *My Breakfast Smoothie*)  
- **Multi-Tab UI:** Switch between Search, My Meals, and Cart without data loss  

---

### 🧠 Goal Setup (Auto & Manual)
Flexible goal configuration for beginners and advanced users:

- **Automatic Calculator:** Uses the **Mifflin-St Jeor Equation** to calculate BMR & TDEE  
- **Goal Presets:** Cutting / Maintenance / Bulking  
- **Manual Control:** Fully customizable calorie & macro targets  

---

## ✨ Technology Stack

| Component          | Tech Used                                   |
|-------------------|----------------------------------------------|
| 💻 Language        | Java (Android SDK)                           |
| 🎨 Architecture    | MVVM (Model-View-ViewModel)                 |
| 🎛️ Navigation      | Jetpack Navigation Component (Single Activity) |
| ☁️ Database        | Firebase Firestore (NoSQL)                  |
| 🔐 Authentication  | Firebase Authentication                    |
| 🌐 Networking      | HttpURLConnection + JSON (REST APIs)        |
| 🖼️ Image Handling  | Glide                                      |
| 📈 Data Visualization | MPAndroidChart Library                 |

---

## 🛠️ Developer & Project Details

- Developed by **Upendra Dasanayaka**  
- High-performance **native Android application**  
- Built using modern **HCI (Human-Computer Interaction)** principles  
- Includes a **Web-based Admin Panel** for cloud food database management  

---

## 🚀 Why GoalGains?

### 🔄 1. Shared ViewModel Strategy
Activity-scoped ViewModels enable seamless data flow between tabs  
(Search → Cart → Dashboard) with zero lag.

### 📡 2. Real-Time Cloud Sync
Powered by Firebase — logs, progress, and goals sync instantly across devices.

### 🧠 3. Data-Driven Motivation
Visual history charts and streak systems drive long-term consistency and discipline.

---
