# PROMETHEUS ARCHITECTURE PLAN (100-Year Vision)

## 1. The Core Philosophy: "Fluidity & Rock-Solid State"
The application must feel like an extension of the user's mind—instantaneous, predictable, and unbreakable.

### 🏛️ Pillars of Stability
1.  **Unidirectional Data Flow (UDF)**: Strict state management. The UI *never* changes state directly; it emits events. The ViewModel processes events and emits a single `ViewState`.
2.  **Immutable Domain Models**: All password entries and user settings must be immutable data classes. Modification creates copies.
3.  **Database as Single Source of Truth**: The Repository observes Room (`Flow`) and emits to ViewModel. ViewModel holds NO state other than the latest emission.

## 2. Technical Roadmap

### Phase 1: The "Speed of Light" Refactor (Immediate)
- [x] **Debouncing**: Search inputs must wait 300ms before querying IO.
- [x] **Lazy Hashing**: PBKDF2 is slow. Move it to `Dispatchers.Default` and cache results where safe (Session Scope).
- [ ] **Render Optimization**: Use `key()` in LazyColumn to avoid unnecessary recompositions.
- [ ] **Baseline Profiles**: Generate AOT compilation profiles to reduce startup time by 40%.

### Phase 2: "Fortress" Security
- [ ] **Memory Wiping**: `CharArray` for passwords instead of `String` (String stays in heap until GC). Manually zero-out arrays after use.
- [ ] **Secure Window**: Prevent screenshots (`FLAG_SECURE`) and recent apps thumbnails.
- [ ] **Auto-Lock**: Idle timer hook in `MainActivity` onUserInteraction override.

### Phase 3: "Enterprise" Features
- [x] **Data Sovereignty (Implemented)**: Full JSON Import/Export engine locally (Settings Screen).
- [ ] **Sync Engine**: Encrypted sync to Google Drive / WebDAV.
- [ ] **Audit Log**: Local encrypted log of every view/edit/delete action.

### Phase 4: CI/CD & Quality
- [ ] **LeakCanary**: Automated memory leak detection in debug builds.
- [ ] **Benchmark Tests**: Macrobenchmark tests to ensure frame timing < 16ms (60fps).

## 3. Immediate "Smoothness" Wins
1.  **Shared Element Transitions**: Hero animations when opening a password card.
2.  **Haptic Feedback**: Subtle vibration on copy/save actions.
3.  **Skeleton Loading**: Shimmer effect instead of spinners.

## 4. Disaster Recovery
- [x] **Backup & Restore**: JSON based import/export allows users to own their data.
- [ ] **Encrypted Backups**: Option to encrypt the exported JSON with a separate key.

Signed,
*Antigravity, Principal Architect*
