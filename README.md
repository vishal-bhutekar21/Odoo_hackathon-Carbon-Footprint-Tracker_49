CarbonView - TechBuddies Team 49
App Name
CarbonView

Problem Statement
3. Carbon Footprint Tracker

Problem: Individuals and organizations lack comprehensive tools to accurately measure, understand, and reduce their carbon emissions.

Example Idea: Develop an integrated carbon footprint tracking ecosystem:

IoT Integration: Create a multi-device platform that collects real-time emissions data from smart home devices, vehicles, appliances, and personal electronics.
Personalized Emissions Reporting: Generate detailed, user-friendly visualizations of carbon impact with actionable recommendations for reducing individual and organizational carbon footprints.
Sustainability Incentives: Design a reward system that gamifies carbon reduction, offering credits, challenges, and community recognition for achieving emissions targets.
Our Solution
CarbonView, developed by Team TechBuddies (Team No. 49), is an Android application designed to tackle this challenge by offering a robust tool to track and manage carbon emissions. We’ve addressed all aspects of the problem statement by enabling users to input data across various everyday activities, calculate their carbon footprint, categorize it into Scope 1, 2, and 3 emissions, and receive tailored suggestions to reduce their impact. The app organizes data by month and provides visual insights to foster sustainable habits.

Team Members
Vishal Bhutekar -  Android Developer
Karan Bankar - Android Developer
Chaitanya Kakde - Android Developer
Satwik Mahajan - UI/UX Designer
Features
1. IoT Integration
How We Did It:

We created a system where users can manually input data related to their daily activities—such as fuel usage, flights taken, shipping logistics, and industrial production—mimicking real-time data collection from connected devices like vehicles or smart appliances. For flights, we fetch live data from an external service to enhance accuracy, simulating how IoT could pull emissions data from travel systems. All calculated emissions are stored in a cloud database, organized by category and month (e.g., "2025-March"), allowing seamless tracking as if synced from multiple sources.

2. Personalized Emissions Reporting
How We Did It:

After users enter their data and trigger a calculation, we present their carbon footprint in multiple units (grams, pounds, kilograms, metric tons) alongside the date of estimation, making it easy to grasp their impact. We display standard emission rates for each activity (e.g., fuel per liter, production per unit) to clarify how results are derived. The app also generates graphical charts showing monthly totals, helping users visualize trends over time. Data is classified into three categories—Scope 1 (direct emissions), Scope 2 (indirect from purchased energy), and Scope 3 (other indirect emissions)—to provide a structured understanding of their footprint.

Scope Classification:
Scope 1: Direct emissions from owned sources, like fuel burned in vehicles (Fuel category).
Scope 2: Indirect emissions from electricity or energy use (not directly calculated here but could be inferred from industrial production).
Scope 3: Other indirect emissions, such as those from flights, shipping, or supply chains (Flight, Transport, Industry categories).
3. Sustainability Incentives
How We Did It:

To encourage reduction, we designed an interactive flow where users can choose to save their calculated emissions to the cloud after each calculation or discard them, promoting active engagement with their data. Saving emissions updates a running total for the month, acting as a challenge to keep totals low. This gamifies the experience by rewarding users with a sense of progress as they see their footprint decrease over time, laying the groundwork for future features like earning credits or community recognition for sustainable choices.

4. Suggestions Based on Footprint
How We Did It:

Based on the calculated emissions and their Scope classification, we provide tailored suggestions to help users reduce their carbon footprint. These are integrated into the app’s feedback mechanism and updated dynamically as users track more data:

High Scope 1 (Fuel Usage): "Consider switching to electric vehicles or reducing driving by carpooling."
High Scope 3 (Flights): "Opt for train travel when possible or consolidate trips to lower flight frequency."
High Scope 3 (Transport): "Use sea freight over air freight for lower emissions in shipping."
High Scope 3 (Industry): "Adopt energy-efficient machinery or source materials locally to cut production emissions."
General Tip: "Offset your footprint by supporting renewable energy projects or planting trees."
These suggestions appear alongside the results, empowering users with actionable steps tied to their specific impact.
Tech Stack
Languages: Kotlin, Java
UI Framework: XML (Android Layouts)
Libraries & APIs:
Carbon Interface API: For real-time flight emission calculations.
MPAndroidChart: For creating visual charts of emission trends.
Retrofit: For retrieving data from external APIs efficiently.
Firebase Realtime Database: For cloud storage and real-time data updates.
Tools: Android Studio
Video Demonstration
Check out our app demo here:

CarbonView Demo Video

https://youtu.be/zsv2l4NFGkM?si=-ChfzpXcTmBg6M9F


google drive links 

https://drive.google.com/drive/folders/1If32rKDsayGV7HmKXEOMW0NhDbTrmgnH?usp=sharing

