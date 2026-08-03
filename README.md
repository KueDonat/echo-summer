# 🌊 Echo Summer — Visual Novel & Rhythm Game

**Echo Summer** adalah game bergenre *Visual Novel* dan *Rhythm Game* interaktif yang dibangun menggunakan framework **libGDX (Java 17)**. 

Proyek ini dirancang secara khusus untuk **Sidang Mata Kuliah Struktur Data**, di mana seluruh logika struktur data inti diimplementasikan secara **Kustom (*from scratch*)** menggunakan prinsip **Abstract Data Type (ADT)** tanpa bergantung pada kelas koleksi bawaan Java (seperti `java.util.Stack`, `java.util.Queue`, atau `java.util.HashMap`) untuk pengelolaan state permainan utama.

---

## 📌 Ringkasan Struktur Data yang Digunakan

| No | Struktur Data | ADT Interface | Kelas Implementasi | Prinsip / Konsep | Kompleksitas (Avg/Best) | Penggunaan Utama dalam Game |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | **Stack** | [`IStack<T>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/adt/IStack.java) | [`CustomStack<T>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomStack.java) | LIFO (*Last-In, First-Out*) dengan Singly Linked Node | Push: $O(1)$<br>Pop: $O(1)$ | Riwayat dialog (*Dialogue History / Backtrack / Undo*) |
| 2 | **Queue** | [`IQueue<T>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/adt/IQueue.java) | [`CustomQueue<T>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomQueue.java) | FIFO (*First-In, First-Out*) dengan Head-Tail Node Chain | Enqueue: $O(1)$<br>Dequeue: $O(1)$ | Urutan kemunculan & pemrosesan note pada Rhythm Game |
| 3 | **Doubly Linked List** | [`ILinkedList<T>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/adt/ILinkedList.java) | [`CustomLinkedList<T>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomLinkedList.java) | Doubly Linked List dengan optimasi pencarian dari 2 arah | Add: $O(1)$<br>Get: $O(n/2)$ | Mengelola daftar Quest aktif & Inventory Item |
| 4 | **Hash Table** | [`IHashTable<K,V>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/adt/IHashTable.java) | [`CustomHashTable<K,V>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomHashTable.java) | Hash Table dengan *Separate Chaining* & *Dynamic Rehashing* | Put: $O(1)$<br>Get: $O(1)$ | Pencarian cepat node dialog cerita berdasarkan Key ID |
| 5 | **N-ary Tree** | *(N-ary Tree)* | [`CustomTree<T>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomTree.java) | Hierarki Pohon *N-ary* dengan Pre-Order Traversal | Insert Child: $O(1)$<br>Traversal: $O(V)$ | Percabangan narasi / alur cerita pilihan visual novel |
| 6 | **Graph** | [`IGraph<V>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/adt/IGraph.java) | [`LocationGraph<V>`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/LocationGraph.java) | Adjacency List (Weighted/Bidirectional Graph) + BFS | BFS Pathfinding: $O(V + E)$ | Peta konektivitas lokasi permainan & rute terpendek |

---

## 📑 Penjelasan Detail Implementasi Struktur Data

### 1. 📚 Custom Stack (`CustomStack<T>`) — LIFO
- **Lokasi Code**: [`CustomStack.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomStack.java)
- **Struktur Internal**: Menggunakan node berarah tunggal (*singly linked node chain*) dengan pointer `top`.
- **Operasi Utama**:
  - `push(T item)`: Menambahkan node baru ke atas stack ($O(1)$).
  - `pop()`: Mengambil dan menghapus node paling atas ($O(1)$).
  - `peek()`: Melihat elemen paling atas tanpa menghapusnya ($O(1)$).
- **Penggunaan dalam Game**:
  - Digunakan pada [`GameplayScreen.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/GameplayScreen.java) sebagai `dialogueHistoryStack`.
  - Setiap kali dialog baru dibaca, node dialog lama di-*push* ke stack. Saat pemain membuka riwayat dialog atau melakukan *backtrack*, elemen di-*pop* dari stack sehingga dialog sebelumnya dapat ditampilkan kembali secara berurutan (*Last-In, First-Out*).

---

### 2. 🎵 Custom Queue (`CustomQueue<T>`) — FIFO
- **Lokasi Code**: [`CustomQueue.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomQueue.java)
- **Struktur Internal**: Menggunakan rantai node dengan pointer `head` dan `tail`.
- **Operasi Utama**:
  - `enqueue(T item)`: Menambahkan elemen di posisi `tail` ($O(1)$).
  - `dequeue()`: Menghapus elemen dari posisi `head` ($O(1)$).
  - `peek()`: Melihat note terdepan yang akan mencapai hit zone ($O(1)$).
- **Penggunaan dalam Game**:
  - Digunakan pada minigame [`RhythmGame.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/RhythmGame.java) sebagai `noteQueue`.
  - Note lagu dimasukkan ke antrean (*enqueue*) sesuai *timestamp* lagu. Saat permainan berlangsung, note yang berada di paling depan (*head*) diperiksa untuk *timing hit*. Ketika note ditekan atau luput (*miss*), note tersebut di-*dequeue*.
  - Sisa antrean note ditampilkan secara *real-time* pada HUD antrean FIFO minigame.

---

### 3. 🔗 Custom Doubly Linked List (`CustomLinkedList<T>`)
- **Lokasi Code**: [`CustomLinkedList.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomLinkedList.java)
- **Struktur Internal**: Memiliki dua pointer pada tiap node, yaitu `prev` dan `next`, serta pointer `head` dan `tail`.
- **Fitur Optimasi**: 
  - Fungsi `getNode(index)` membagi area pencarian: jika `index < size / 2`, penelusuran dimulai dari `head` maju ke depan; jika `index >= size / 2`, penelusuran dimulai dari `tail` mundur ke belakang. Hal ini menghemat waktu pencarian hingga 50%.
- **Penggunaan dalam Game**:
  - Digunakan pada [`GameplayScreen.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/GameplayScreen.java) sebagai `activeQuestList` dan UI list inventory item.
  - Memungkinkan penambahan dan penghapusan quest/item secara dinamis tanpa perlu melakukan alokasi ulang array (*re-allocation*).

---

### 4. 🔑 Custom Hash Table (`CustomHashTable<K, V>`)
- **Lokasi Code**: [`CustomHashTable.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomHashTable.java)
- **Struktur Internal**: Array of Bucket Entries dengan teknik **Separate Chaining** (Linked List pada tiap bucket) untuk penanganan bentrokan (*collision resolution*).
- **Fitur Khusus**:
  - **Dynamic Rehashing**: Ketika *load factor* ($\frac{size}{capacity}$) mencapai **0.75**, ukuran bucket otomatis membesar menjadi $2 \times capacity + 1$ dan seluruh elemen di-*rehash*.
  - Fungsi Hash: `Math.abs(key.hashCode() % capacity)`.
- **Penggunaan dalam Game**:
  - Digunakan pada [`StoryData.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/StoryData.java) dan [`GameplayScreen.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/GameplayScreen.java) sebagai `storyNodeHashTable`.
  - Menyimpan seluruh `DialogueNode` cerita dengan `String ID` sebagai Key (misal: `"node_intro"`, `"node_choice_beach"`). Memungkinkan pencarian node dialog secepat $O(1)$ tanpa iterasi lambat.

---

### 5. 🌳 Custom N-ary Tree (`CustomTree<T>`)
- **Lokasi Code**: [`CustomTree.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/CustomTree.java)
- **Struktur Internal**: Pohon *N-ary* di mana setiap `TreeNode<E>` menyimpan `data`, penunjuk `parent`, serta `List<TreeNode<E>> children` (bisa memiliki banyak cabang anak).
- **Metode Traversal**:
  - `preOrderTraversal()`: Mengunjungi root terlebih dahulu lalu secara rekursif mengunjungi cabang anak dari kiri ke kanan.
  - `findNode(Predicate)`: Mencari node yang memenuhi kondisi tertentu.
- **Penggunaan dalam Game**:
  - Digunakan pada [`StoryData.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/StoryData.java) sebagai `storyDecisionTree`.
  - Membentuk pohon alur alur cerita interaktif (*narrative branching tree*). Setiap pilihan keputusan (*Choice*) cabang cerita menjadi *child node* dari cerita sebelumnya.

---

### 6. 🗺️ Custom Location Graph (`LocationGraph<V>`) & BFS
- **Lokasi Code**: [`LocationGraph.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/ds/LocationGraph.java)
- **Struktur Internal**: **Adjacency List** berbasis `Map<V, List<Edge<V>>>` yang mendukung graf berbobot (*weighted*) dan dua arah (*bidirectional*).
- **Algoritma Pathfinding**:
  - Implementasi **Breadth-First Search (BFS)** melalui metode `findShortestPathBFS(start, target)`.
  - BFS menggunakan antrean Queue dan `parentMap` untuk mencatat jejak jalur terpendek (jumlah hop ter-minimal) antar lokasi.
- **Penggunaan dalam Game**:
  - Digunakan pada [`GameplayScreen.java`](file:///c:/echo%20summer/core/src/main/java/com/echosummer/game/GameplayScreen.java) sebagai `mapNavigationGraph`.
  - Memodelkan titik lokasi pada peta (misal: *Rumah*, *Sekolah*, *Taman*, *Pantai*, *Kafe*) sebagai vertex dan jalan penghubung sebagai edge. Digunakan untuk menghitung rute perjalanan terpendek pemain.

---

## 📁 Struktur Paket Data Structure (`ds`)

```text
core/src/main/java/com/echosummer/game/
├── ds/
│   ├── adt/                  # Antarmuka Abstract Data Type
│   │   ├── IGraph.java       # Interface ADT Graph
│   │   ├── IHashTable.java   # Interface ADT Hash Table
│   │   ├── ILinkedList.java  # Interface ADT Doubly Linked List
│   │   ├── IQueue.java       # Interface ADT Queue
│   │   └── IStack.java       # Interface ADT Stack
│   ├── CustomHashTable.java  # Implementasi Hash Table (Separate Chaining)
│   ├── CustomLinkedList.java # Implementasi Doubly Linked List
│   ├── CustomQueue.java      # Implementasi Queue (Linked Node)
│   ├── CustomStack.java      # Implementasi Stack (Linked Node)
│   ├── CustomTree.java       # Implementasi N-ary Decision Tree
│   └── LocationGraph.java    # Implementasi Graph (Adjacency List + BFS)
├── GameplayScreen.java       # Menggunakan Stack, Linked List, Graph, Hash Table, & Tree
├── RhythmGame.java           # Menggunakan Queue untuk sistem note
└── StoryData.java            # Membangun Hash Table & Tree cerita
```

---

## 💡 Bahan Diskusi / Materi Sidang (Defense Talking Points)

Saat ujian/sidang mata kuliah Struktur Data, Anda dapat menjelaskan poin-poin unggulan berikut:

1. **Mengapa Menggunakan Implementation Kustom (ADT)?**
   - Menjelaskan abstraksi data (*Separation of Concerns*) antara *Interface ADT* (`IStack`, `IQueue`, dll.) dan *Class Implementasi*.
   - Tidak bergantung pada pustaka koleksi Java bawaan untuk membuktikan pemahaman mendalam tentang manipulasi pointer/reference, penanganan memori, dan algoritma dasar.
2. **Penanganan Collision pada Hash Table**:
   - Menjelaskan teknik *Separate Chaining* di mana jika dua Key menghasilkan indeks bucket yang sama, keduanya disimpan dalam linked list pada bucket tersebut.
   - Menjelaskan pentingnya *Load Factor (0.75)* dan proses *Rehashing* untuk menjaga kompleksitas pencarian tetap $O(1)$.
3. **Kesesuaian Struktur Data dengan Kasus Penggunaan Game**:
   - **Queue untuk Rhythm Game**: Prinsip *FIFO (First-In, First-Out)* menjamin bahwa note yang muncul lebih awal akan diproses terlebih dahulu untuk perhitungan skor.
   - **Stack untuk Riwayat Dialog**: Prinsip *LIFO (Last-In, First-Out)* sangat pas untuk fitur *undo/backtrack* dialog karena dialog paling baru ditarik kembali pertama kali.
   - **Tree untuk Branching Story**: Model *N-ary Tree* merepresentasikan cabang pilihan cerita dengan banyak opsi secara alami.
   - **Graph + BFS untuk Map**: Graf merepresentasikan konektivitas wilayah dunia nyata, dan BFS menjamin ditemukannya rute perjalanan terpendek (jalur paling efisien).

---

## 🚀 Cara Menjalankan Program (Build & Run)

Prasyarat: JDK 17 atau yang lebih baru.

### Menggunakan Gradle Wrapper:
```bash
# Menjalankan aplikasi Desktop (LWJGL3)
./gradlew lwjgl3:run

# Pada Windows Command Prompt / PowerShell:
gradlew.bat lwjgl3:run
```

---

*Dibuat untuk keperluan akademik & sidang Mata Kuliah Struktur Data — Proyek Echo Summer.*
