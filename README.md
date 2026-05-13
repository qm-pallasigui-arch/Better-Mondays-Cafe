# Better Mondays Cafe

A Java Swing desktop application for a small café that combines:
- **Login/Registration** with role-based access control
- **Point-of-Sale (POS) ordering**
- **Inventory management** with batch/lot tracking and analytics
- **Low-stock monitoring and sales tracking**
- **Staff shift management**
- **Menu maintenance, search, help, and reporting tools**

## Tech Stack

- **Language:** Java (configured for Java 25 in `pom.xml`)
- **Build tool:** Maven
- **UI:** Java Swing (NetBeans GUI Builder, `.form` files)
- **Layout dependency:** `org.netbeans.external:AbsoluteLayout`
- **Data storage:** SQLite (`data/coffee-cafe.db`) via `org.xerial:sqlite-jdbc`; legacy `users.txt` credentials are migrated automatically on first run
- **Roles:** `ADMIN` (full access) and `STAFF` (limited access)

## Project Structure

```text
src/main/java/
  loginregister/
    Login.java              # login screen + app entry flow
    Register.java           # sign-up screen with password validation
    UserDataManager.java    # user CRUD, role enum (ADMIN/STAFF), delegates to SQLite
    PasswordHasher.java     # SHA-256 + salt hashing
    UserAccount.java        # user model (username, role, createdAt)

  pos/
    POSSystem.java          # main window (tabs: POS, Inventory, Monitoring + admin tabs)
    Menu.java               # singleton menu catalog and pricing by variant
    MenuItem.java           # abstract base for menu items
    CoffeeItem.java
    NonCoffeeItem.java
    FruitTeaItem.java
    HerbalTeaItem.java      # item categories + ingredient deduction rules

  inventory/
    Inventory.java          # singleton inventory store + initialization
    InventoryItem.java      # inventory model + low-stock check
    InventoryBatch.java     # batch/lot model with expiry date for FEFO handling
    analytics/
      AbcClassifier.java        # ABC analysis (A=top 80%, B=next 15%, C=last 5%)
      EoqCalculator.java        # Economic Order Quantity formula
      InventoryPolicyService.java # EOQ + ABC wrappers with defaults
    validation/
      InventoryBatchValidator.java  # validates quantity and expiry inputs

  monitoring/
    Monitoring.java         # fills monitoring/sales tables
    SalesRecord.java        # sales row model

  controller/
    OrderController.java        # persists completed transactions via SalesRepository
    InventoryController.java    # builds inventory rows with ABC classification
    InventoryRowView.java       # view model for an inventory table row

  persistence/
    AppDatabase.java            # SQLite connection factory (data/coffee-cafe.db)
    SchemaInitializer.java      # creates all tables on first run
    Phase2Bootstrap.java        # seeds SQLite from in-memory catalog if empty
    LegacyUserMigration.java    # imports legacy users.txt into SQLite once
    AccountRoleRepository.java  # interface for role queries
    InventoryRepository.java    # interface for inventory data
    MenuRepository.java         # interface for menu data
    SalesRepository.java        # interface for sales records
    StaffShiftRepository.java   # interface for staff shifts
    UserRepository.java         # interface for user accounts
    sqlite/
      SQLiteInventoryRepository.java
      SQLiteMenuRepository.java
      SQLiteSalesRepository.java
      SQLiteStaffShiftRepository.java
      SQLiteUserRepository.java

  staff/
    StaffShift.java         # model: staff shift (start/end time, notes)

  ui/
    AppTheme.java                   # shared colour palette + typography
    AboutModule.java                # About dialog panel
    HelpModule.java                 # tabbed help panel (quick-start, ordering, inventory, reports, FAQ)
    SearchModule.java               # cross-entity search panel
    MenuMaintenancePanel.java       # admin panel for editing menu item prices
    InventoryRegistrationPanel.java # admin panel for adding inventory batches
    InventoryGuidePanel.java        # read-only inventory guide
    StaffPanel.java                 # admin/staff shift tracking + role management

  util/
    BackupManager.java      # copies SQLite DB to data/backups/ with timestamp
    ReportExporter.java     # exports sales records to CSV

src/main/resources/
  images/logo.png
  help/quick-start.txt
  help/ordering.txt
  help/inventory.txt
  help/reports.txt
  help/faq.txt

src/test/java/
  controller/OrderControllerTest.java
  inventory/analytics/AbcClassifierTest.java
  inventory/analytics/EoqCalculatorTest.java
  inventory/validation/InventoryBatchValidatorTest.java

data/coffee-cafe.db      # SQLite database (auto-created on first run)
users.txt                # legacy credential file (migrated to SQLite on startup)
pom.xml                  # Maven configuration
```

## How the App is Organized

1. **Authentication & Roles**
   - `Login` verifies credentials via `UserDataManager` (SQLite-backed, SHA-256 hashed).
   - `Register` validates password rules, assigns a role (`ADMIN` or `STAFF`), and saves through `UserDataManager`.
   - Legacy `users.txt` credentials are imported into SQLite automatically via `LegacyUserMigration`.
   - `ADMIN` users see additional tabs (Menu Maintenance, Inventory Registration, Staff, Search, Help, About).

2. **POS Flow**
   - `POSSystem` drives the ordering UI and cart table.
   - Menu category buttons reveal product buttons.
   - Each product uses `Menu` to resolve variant pricing (`Hot`, `Regular Iced`, `Large Iced`).
   - Totals are computed with VAT and cash/change handling.
   - Payment generates a receipt dialog and persists the transaction via `OrderController` → `SalesRepository`.

3. **Inventory + Monitoring**
   - Inventory is loaded from the `Inventory` singleton and seeded into SQLite by `Phase2Bootstrap`.
   - On payment, ingredients are deducted using each `MenuItem` category's ingredient map.
   - `InventoryController` builds table rows enriched with ABC classification from `InventoryPolicyService`.
   - `InventoryBatch` supports lot-level tracking with expiry dates (FEFO).
   - Monitoring tab highlights low-stock ingredients and shows sales entries.

4. **Admin Tools** *(ADMIN role only)*
   - **Menu Maintenance** – edit menu item prices stored in SQLite.
   - **Inventory Registration** – add inventory batches with optional expiry dates.
   - **Staff Panel** – view/start/end shifts and promote/demote user roles.
   - **Search** – query across entities (sales, menu, inventory).
   - **Reports** – export sales to CSV via `ReportExporter`; backup the database via `BackupManager`.
   - **Help / About** – in-app documentation tabs and application info.

## Build and Run

### Requirements
- JDK **25** (project uses `maven.compiler.release=25`)
- Maven 3.9+

### Commands
```bash
mvn clean package
mvn exec:java -Dexec.mainClass=loginregister.Login
```

The SQLite database (`data/coffee-cafe.db`) is created automatically on first run.

## Default Credentials

The repo ships with `users.txt` containing:
- `admin / admin`

These are migrated into the SQLite store on first launch.

## Notes

- Most UI code in `Login`, `Register`, and `POSSystem` is generated by the NetBeans Form Editor.
- Unit tests live under `src/test/java` and cover `OrderController`, `AbcClassifier`, `EoqCalculator`, and `InventoryBatchValidator`.
