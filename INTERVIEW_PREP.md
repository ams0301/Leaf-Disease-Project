# Plant Disease Detection App — Complete Interview Preparation Guide

> A full-stack mobile + ML project: Android (Kotlin) app -> Node.js/Express backend -> Python/TensorFlow CNN serving.
> This guide covers every component in depth, the bugs found and fixed (great war stories), design trade-offs,
> and the interview questions students most often forget to prepare.
>
> **New to this project or to these concepts? Read `START_HERE.md` first** — it teaches the whole
> system as a simple story, defines every term in plain English, and gives you word-for-word scripts.

---

## Table of Contents

1. [How to use this guide](#1-how-to-use-this-guide)
2. [The pitch: 30 seconds and 2 minutes](#2-the-pitch)
3. [Architecture at a glance](#3-architecture-at-a-glance)
4. [The full request lifecycle (memorize this)](#4-the-full-request-lifecycle)
5. [Numbers you must be able to quote](#5-numbers-you-must-be-able-to-quote)
6. [Deep dive: Android app](#6-deep-dive-android-app)
7. [Deep dive: Node.js backend](#7-deep-dive-nodejs-backend)
8. [Deep dive: ML pipeline and model](#8-deep-dive-ml-pipeline-and-model)
9. [Design decisions and trade-offs](#9-design-decisions-and-trade-offs)
10. [Bugs found and fixed — war stories](#10-bugs-found-and-fixed--war-stories)
11. [Android interview Q&A](#11-android-interview-qa)
12. [Backend interview Q&A](#12-backend-interview-qa)
13. [ML interview Q&A](#13-ml-interview-qa)
14. [System design and scaling Q&A](#14-system-design-and-scaling-qa)
15. [Security Q&A](#15-security-qa)
16. [Testing and reliability Q&A](#16-testing-and-reliability-qa)
17. [The questions students forget to prepare](#17-the-questions-students-forget-to-prepare)
18. [Rapid-fire cheat sheet](#18-rapid-fire-cheat-sheet)
19. [Self-test mock interview](#19-self-test-mock-interview)
20. [Day-before checklist and glossary](#20-day-before-checklist-and-glossary)

---

## 1. How to use this guide

- **Section 2 and 4 are mandatory.** If you can deliver the pitch and recite the request lifecycle smoothly,
  you can answer 70% of any project round.
- **Section 5** — interviewers probe for specifics. Saying "about 4 million parameters" or "14 disease classes"
  signals real ownership. Never say "I don't remember" about YOUR OWN project's numbers.
- **Section 10** — the bug war stories are your behavioral-round ammunition. Practice 3 of them out loud in
  STAR format (Situation, Task, Action, Result).
- **Section 17** — read twice. These are the questions that separate candidates who built the project from
  candidates who copy-pasted it.
- All file references point to the actual repo files so you can open and trace every claim.

---

## 2. The pitch

### 30-second version

> "I built a plant disease detection app. The user takes or picks a photo of a leaf on an Android app; the image
> is uploaded to a Node.js backend, which hands it to a Python service running a TensorFlow CNN — a MobileNet
> backbone with three output heads that simultaneously predict the plant (10 classes), the disease category
> (fungal/viral/bacterial/healthy), and the specific disease (14 classes). The model runs test-time augmentation —
> 24 crops and rotations of the photo with majority voting — and the result comes back to the app as JSON and is
> shown in a popup. I recently debugged the whole pipeline end to end and fixed real production bugs in the
> process — stdout corruption, a model metadata typo, and a client timeout that made the app always fail."

### 2-minute version

> "The system has three parts.
>
> **The Android app (Kotlin)** has a camera screen built on CameraX, a gallery that groups the device's photos
> by recency — recent, last week, by month, by year — with sticky section headers, and a submit screen that
> uploads the chosen image with Retrofit as a multipart request and displays the prediction in a popup.
>
> **The backend (Node.js/Express)** accepts the upload via Multer, saves it to a temp directory, spawns a Python
> process, and communicates with it over a strict contract: arguments in, one line of JSON on stdout, logs to
> stderr. The Python process deletes the uploaded file when done.
>
> **The ML service (Python/TensorFlow)** loads a Keras SavedModel — an ImageNet-pretrained MobileNet V1 with
> three small Dense heads for plant, category, and disease. Preprocessing resizes the image to 256 on the short
> side, then takes corner and center crops at 224x224, plus one randomly augmented center crop, across four
> rotations — 24 tensors per photo. We take the argmax per head per crop, then the majority vote across crops,
> and map class indices to human labels.
>
> Verified end to end: a real lettuce photo returns plant Lettuce, category Healthy, in about 15-20 seconds
> per request. Along the way I fixed a dozen real bugs — the most interesting being that the app could never
> have worked against the server: OkHttp's default 10-second read timeout was shorter than the model's
> inference time."

---

## 3. Architecture at a glance

```
+---------------------+        +------------------------+        +---------------------------+
|   ANDROID APP       |        |   NODE.JS BACKEND      |        |   PYTHON ML WORKER        |
|   (Kotlin)          |        |   (Express)            |        |   (TensorFlow/Keras)      |
|                     |        |                        |        |                           |
| CameraX screen      |        | POST /image            |        | scripts/                  |
| MediaStore gallery  |  HTTP  |  - multer -> temp file  | spawn  |  image_preprocess.py      |
| Submit screen       | ------>|  - guard req.file      | ------>|  - load SavedModel        |
| Retrofit + Gson     | multi- |  - spawn('python',     | argv   |  - resize 256, crop 224  |
| Coroutines + VMs    | part   |     [script, path,     |        |  - 4 rotations x 6 crops  |
| PopupWindow result  |        |      idx1..idx3])      |        |  - predict 3 heads        |
|                     |        |  - accumulate stdout    | JSON   |  - argmax + majority vote |
|                     | <------|  - res 200 / 400 / 500 | <----- |  - print JSON to stdout   |
+---------------------+  JSON  +------------------------+ stdout +---------------------------+
     ^                                                              |
     |                                                       deletes uploaded file
  PopupWindow shows                                            when done
  plant / category / disease
```

### Component map (file-level)

| Layer | Key files | Role |
|---|---|---|
| App — screens | `MainActivity.kt`, `CameraActivity.kt`, `GalleryActivity.kt`, `SubmitActivity.kt` | Capture / camera test / gallery / submit-and-result |
| App — networking | `APIService.kt`, `PlantData.kt` | Retrofit interface + response model |
| App — UI support | `GalleryAdapter.kt`, `GalleryItemAdapter.kt`, `GalleryItemDecoration.kt`, `GalleryPeekViewAdapter.kt`, `DetailPopupWindow.kt` | Sectioned gallery with sticky headers, result popup |
| App — state | `CameraViewModel.kt`, `GalleryViewModel.kt` | Group device images into RECENT/LAST WEEK/months/years |
| Backend | `app.js`, `bin/www`, `routes/image.js`, `routes/index.js`, `constants.js` | Express wiring, port handling, the prediction route |
| ML | `scripts/image_preprocess.py`, `data/plant_leaf_model/` | Preprocessing, augmentation, inference, label mapping |
| Labels | `data/num_to_label_file.json`, `data/label_to_num_file.json` | Class-index <-> name maps for the 3 heads |
| Deploy (static only) | `firebase.json`, `public/` | Firebase Hosting config for the static page — **not** the API |

### Tech stack summary

| Concern | Choice | Version |
|---|---|---|
| Language (app) | Kotlin | 1.5.0 |
| Build | Gradle 6.7.1 + AGP 4.2.0, build-tools 30.0.1 | compile/target SDK 30, minSdk 23 |
| Networking | Retrofit + OkHttp + Gson | 2.9.0 (converter 2.3.0) |
| Camera | CameraX (camera2 wrapper) | 1.0.1, camera-view 1.0.0-alpha27 |
| Async | Kotlin coroutines | 1.4.1 |
| Images | Glide | 4.12.0 |
| UI toolkit | AndroidX + Material Components | appcompat 1.3.0, material 1.4.0 |
| Server | Node.js + Express | Express 4.2x |
| Uploads | Multer | 1.4.5-lts.2 |
| ML runtime | Python + TensorFlow | TF 2.10.1 (last native-Windows TF), numpy <2, Pillow |
| Model format | Keras SavedModel directory | `saved_model.pb` + `variables/` |

---

## 4. The full request lifecycle

**Practice reciting this out loud. It is the single most asked question: "Walk me through what happens
when the user taps Submit."**

### Step-by-step: user taps Submit on the Android app

1. **Image in hand** — `SubmitActivity` received the image `Uri` via Intent extra
   (`ImageClickListener.startNewActivity`). If the Uri points to a content provider (gallery), the app copies
   the bytes to a temp file in the app-specific external dir (`getFile()`, `File.createTempFile`), because
   Retrofit needs a real file handle.

2. **Build the request** — `sendImageToServer(file)` wraps the file in `RequestBody` with MIME
   `image/jpeg`, then `MultipartBody.Part.createFormData("image", file.name, requestBody)`. The field name
   **must** be `"image"` because the backend does `upload.single('image')`.

3. **Suspend call on a coroutine** — `APIService.sendImage()` is a `suspend fun`. It is launched in
   `activityScope.launch` (a `CoroutineScope(SupervisorJob())`); UI updates hop back with
   `launch(Dispatchers.Main)`. The Retrofit instance was built with **explicit OkHttp timeouts
   (connect 30s / read 60s / write 30s)** — critical, because the server needs 15-20s to answer and OkHttp's
   default read timeout is 10s.

4. **HTTP multipart POST** — OkHttp sends `POST /image` with `Content-Type: multipart/form-data; boundary=...`
   and a binary body: headers + JPEG bytes for the `image` part.

5. **Express middleware chain** — `morgan` logs, `express.json()`/`urlencoded` parse (no-op for multipart),
   then the router for `/image` runs `upload.single('image')` (Multer), which streams the file to
   `Backend/image/<32-hex-random-name>` and fills `req.file` (path, originalname, mimetype, size...).

6. **Guard** — if no file part was sent, the route returns `400 {"error": "No image file was uploaded"}`
   instead of crashing on `req.file.path`.

7. **Spawn the ML worker** — `spawn('python', ['scripts/image_preprocess.py', <temp path>,
   'plant_name', 'disease_category', 'disease_name'])`. Spawn with an argv array (no shell) — no shell
   injection possible. The three label names come from `constants.js` and become BOTH the model head
   selectors AND the JSON response keys.

8. **Python preprocessing** — `Image.open(path).convert('RGB')` (grayscale/alpha images normalized),
   then rotations 0/90/180/270. Each rotated image is resized so the **short side is 256** (aspect ratio
   kept, floored), then cropped: 4 corners + center at **224x224**, plus one random-effect center crop
   (flip / brightness / contrast / sharpness / color). Total **24 tensors**.

9. **Inference** — `model.predict(images, verbose=0)` returns a dict:
   `{'plant_name': (24,10), 'disease_category': (24,4), 'disease_name': (24,14)}`.
   (Model metadata contains a typo'd layer name `disease_catetgory` — the code resolves heads robustly:
   exact dict-key match first, close-match fallback.)

10. **Postprocess** — per head: `argmax` over each of the 24 predictions -> class indices -> **majority vote**
    (`max(set(pred), key=pred.count)`) -> map index to label via `num_to_label_file.json`.

11. **IPC back** — the script prints ONE line of JSON to **stdout** (progress bar suppressed with
    `verbose=0`), logs to stderr, and **deletes the uploaded temp file**.

12. **Node collects stdout** — chunks are accumulated in the `data` handler; on `close`, one `JSON.parse`
    inside try/catch. `200` with the prediction JSON, or `500` if the worker failed / output unparsable.
    A `responded` flag prevents double responses if both `error` and `close` fire.

13. **Back on Android** — Gson maps `{"plant_name": ..., "disease_category": ..., "disease_name": ...}` onto
    `PlantData` (`@SerializedName` annotations). On `Dispatchers.Main`: hide the progress bar, and
    `DetailPopupWindow` shows either the healthy popup (category == "Healthy") or the disease popup
    (plant + category + disease). Any exception -> toast "Some Error Occurred".

### Failure modes you should name unprompted

| Failure | Behavior today |
|---|---|
| No image in request | `400` JSON error (route guard) |
| Python/TensorFlow missing | spawn `error` event -> `500` JSON error |
| Model throws / bad output | `close` + parse failure -> `500` JSON error |
| Server unreachable / timeout | Retrofit throws inside `sendImageToServer`, caught, `null` -> toast |
| Grayscale or PNG-with-alpha upload | normalized via `.convert('RGB')` before inference |
| Empty device gallery | `sortImageUri` early-returns; no crash |

---

## 5. Numbers you must be able to quote

### Classes (know these by heart)

- **10 plants**: Basil, Beans, Lettuce, Mint, Rosemary, Chard, Kale, Oregano, Parsley, Chives
- **4 categories** (indices): 0 Fungal, 1 Healthy, 2 Viral, 3 Bacterial
- **14 diseases**: Downy Mildew, Cercospora Leaf Spot, *(empty — used for healthy plants)*, Alternaria Leaf
  Spot, Bean Rust, Mosaic Virus, Powdery Mildew, Wilt and Leaf Blight, Septoria Blight, Leaf Spot, Mint Rust,
  Black Rot, Alternaria Leaf Blight, Septoria Leaf Spot
- Note the **empty-string disease label at index 2** — healthy plants have no disease name; the app shows
  the healthy popup instead.

### Pipeline numbers

| Quantity | Value |
|---|---|
| Resize (short side) | 256 px |
| Model input / crop size | 224 x 224 x 3 |
| Crops per rotation | 4 corners + 1 center + 1 augmented center = 6 |
| Rotations | 4 (0, 90, 180, 270 degrees) |
| Tensors per photo | **24** (4 x 6) |
| Aggregation | per-head majority vote across the 24 predictions |
| Model params | **4,281,892** (~4.28M; 4,260,004 trainable) |
| Model on disk | ~51 MB (saved_model.pb 2.2 MB + variables 48.9 MB) |
| Model load time (cold, CPU) | ~6.6 s |
| Inference time (24-crop batch, CPU) | ~1.2 s |
| Full HTTP request latency | **~15-20 s** (model load dominates — loaded per request) |
| Debug APK size | ~5.6 MB |
| Server port | 3000 (field `"image"`, base URL `http://192.168.1.8:3000/`) |

### Model architecture (verified by loading it)

```
Input (224,224,3)
  -> MobileNet V1 backbone (depthwise separable convs, conv1 ... conv_pw_13, ImageNet pretrained)
  -> GlobalAveragePooling2D -> Reshape -> Dropout
  -> conv_preds Conv2D(1000)          <- ImageNet 1000-class top KEPT as a feature layer
  -> Reshape (1000)
  -> three parallel Dense heads:
       plant_name      Dense(10)   10,010  params
       disease_category Dense(4)    4,004   params   (layer literally named 'disease_catetgory' - typo)
       disease_name    Dense(14)   14,014  params
```

Talking point: this is **transfer learning where the ImageNet logits (1000-dim) act as features** and three
small heads branch from them — instead of the more common "cut the top, add one classifier" design.

---

## 6. Deep dive: Android app

Package: `com.seedbx.leafdisease` — 4 activities, no fragments.

### Screens and responsibilities

| Screen | File | What it does |
|---|---|---|
| Main | `MainActivity.kt` | Test/demo screen: TakePicture + GetContent contracts, shows picked image, uploads with `runBlocking` (see known debt) |
| Camera | `CameraActivity.kt` | CameraX preview + capture; in-app gallery data preload |
| Gallery | `GalleryActivity.kt` | Device photos grouped by recency with sticky headers |
| Submit | `SubmitActivity.kt` | Preview chosen image, upload with Retrofit, show result popup |

### Manifest facts (`AndroidManifest.xml`)

- Permissions: `INTERNET`, `CAMERA`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`
- `android:usesCleartextTraffic="true"` — required because the server URL is `http://` (LAN, no TLS)
- `MainActivity` is the launcher and has `android:exported="true"` (**required on Android 12+** for any
  component with an intent filter)
- Other activities are not exported (safe default false)

### Camera flow (CameraX, `CameraActivity.kt`)

1. `ProcessCameraProvider.getInstance(this)` returns a `ListenableFuture`; add a listener on
   `ContextCompat.getMainExecutor(this)`.
2. Build `Preview` (surface provider = `PreviewView.surfaceProvider`), `ImageCapture` use case, and select
   `CameraSelector.DEFAULT_BACK_CAMERA`.
3. `cameraProvider.unbindAll()` then `bindToLifecycle(this, selector, preview, imageCapture)` — **lifecycle-aware**:
   camera opens/closes automatically with the activity lifecycle.
4. Capture: `takePicture(OutputFileOptions, mainExecutor, OnImageSavedCallback)` writes a JPEG into
   `getOutputDirectory()` (= `externalMediaDirs/<app_name>`, app-specific so **no storage permission needed**).
5. Single-thread `cameraExecutor` (shut down in `onDestroy`); camera permission requested at runtime,
   re-checked in `onRequestPermissionsResult`.

Layout note worth telling: in `camera_view.xml` the `PreviewView` must be declared **before** the
capture/flash/switch buttons. Later children draw on top in Android — with the preview last it covered the
buttons and they were invisible/unclickable. (Real bug we fixed.)

### Gallery flow (MediaStore + custom sorting)

1. `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` query for `_ID` + `DATE_TAKEN`, sorted `DATE_TAKEN DESC`.
2. Each row becomes a content `Uri` via `Uri.withAppendedPath(source, id)`.
3. `GalleryViewModel.sortImageUri()` buckets them: **RECENT** (current week), **LAST WEEK**, then up to 12
   month buckets, then years. `getLast()` is a **binary search** on the descending-sorted (uri, timestamp)
   array — finds the boundary index where timestamps drop below a cutoff. O(log n) per boundary.
4. The bucket list is a flat `MutableList<Any>` alternating `String` header / `Array<Uri>` items
   (deliberately simple — see Q&A for why that is both pragmatic and hacky).
5. `GalleryAdapter` uses `getItemViewType(position) = position % 2`: even = header TextView, odd = an inner
   `RecyclerView` grid (4 columns, `GalleryItemAdapter` + Glide, click -> `SubmitActivity`).
6. **Sticky headers**: `GalleryItemDecoration` (an `RecyclerView.ItemDecoration`) overrides `onDrawOver`,
   creates/binds the header ViewHolder, and draws it on the Canvas pinned to the top; when the next section
   header scrolls into contact it translates the pinned header upward (`moveHeader` vs `drawHeader`).

### Upload flow (Retrofit, `SubmitActivity.kt`)

```kotlin
interface APIService {
    @Multipart
    @POST("image")
    suspend fun sendImage(@Part imageFile: MultipartBody.Part): PlantData
}

data class PlantData(
    @SerializedName("plant_name")     val plantName: String,
    @SerializedName("disease_category") val diseaseCategory: String,
    @SerializedName("disease_name")   val diseaseName: String?,
)
```

- Retrofit built with a custom `OkHttpClient` — **connect/read/write = 30/60/30 s**. Without this the default
  10 s read timeout killed every request, because the model takes 15-20 s. (This was the bug that meant the
  app had *never* worked end to end against the backend.)
- Request building: `RequestBody.create(MediaType.parse("image/jpeg"), file)` ->
  `MultipartBody.Part.createFormData("image", name, body)` -> suspend call from a coroutine.
- Result: success -> popup (`DetailPopupWindow`, `PopupWindow` shown at `Gravity.CENTER`, touch-to-dismiss);
  failure -> progress bar hidden + toast.

### Concurrency model

- `SubmitActivity`: `CoroutineScope(SupervisorJob())` — network off the main thread via Retrofit's
  suspension; UI updates explicitly on `Dispatchers.Main`. `SupervisorJob` = a failure in one child does not
  cancel siblings.
- `MainActivity`: uses `runBlocking` around the suspend upload — it works (Retrofit suspend does the actual
  HTTP on OkHttp dispatcher threads, so no `NetworkOnMainThreadException`) but **freezes the UI until the
  response arrives**. Known debt; the honest answer is "the demo screen should use the same launch pattern
  as SubmitActivity."
- `by viewModels()` (activity-ktx) for `CameraViewModel` / `GalleryViewModel` — ViewModels survive config
  changes (rotation), so the scanned gallery data is not re-queried on rotate.

### Activity Result API (modern permissions/intents)

- `registerForActivityResult(ActivityResultContracts.TakePicture())` with a `Uri.fromFile(...)` into the
  app-specific dir — the system camera writes to that Uri and returns success.
- `ActivityResultContracts.GetContent()` with `"image/*"` for the gallery picker.
- Replaces the deprecated `onActivityResult` + request-code pattern.

### Known debt in the app (say these before the interviewer finds them)

1. `MainActivity` submits with `runBlocking` (UI freeze for the duration of the call).
2. Captured photos on `CameraActivity` are saved but **not wired into the submit flow** — capture currently
  only logs success. Feature gap.
3. `kotlin-android-extensions` synthetic views are **deprecated** — the correct migration is ViewBinding.
4. `GalleryViewModel` and `CameraViewModel` are near-duplicates — should be one shared class or a
   base class.
5. Server URL is a **hardcoded LAN IP** in `strings.xml` — should be a build config field per environment.
6. Release build has `minifyEnabled false` — R8/ProGuard never exercised.
7. The empty/duplicate `imageUriDateList` paths in `CameraActivity` (gallery preload) are currently unused
   by visible UI (bottom-sheet code commented out) — dead code to clean up.

---

## 7. Deep dive: Node.js backend

### Wiring (`app.js`, `bin/www`)

Middleware order matters and is worth reciting:
`morgan('dev')` (logging) -> `express.json()` -> `express.urlencoded()` -> `cookieParser()` ->
`express.static(public)` -> routers `/`, `/users`, `/image`.

`bin/www` is the classic Express-generator launcher: reads `PORT` env or 3000, `normalizePort`,
friendly `EADDRINUSE`/`EACCES` error messages, `debug('leaf-disease:server')` for the listening log.

### The prediction route (`routes/image.js`) — every line is interview material

```js
let upload = multer({dest: 'image/'})          // disk storage, random 32-hex names, no extension
router.post('/', upload.single('image'), (req, res, next) => {
    if (!req.file) { res.status(400).send({error: 'No image file was uploaded'}); return }
    const pythonProcess = spawn('python',
        ['scripts/image_preprocess.py', req.file.path, constants.idx1, constants.idx2, constants.idx3])
    let stdoutData = ''
    let responded = false
    const respond = (status, body) => { if (responded) return; responded = true; res.status(status).send(body) }
    pythonProcess.stdout.on('data', d => stdoutData += d)     // ACCUMULATE - streams are not messages
    pythonProcess.stderr.on('data', d => console.log(`stderr: ${d}`))
    pythonProcess.on('error', err => respond(500, {error: 'Failed to run prediction process'}))
    pythonProcess.on('close', code => {                       // 'close' = process ended AND stdio drained
        try { respond(200, JSON.parse(stdoutData)) }
        catch (e) { respond(500, {error: 'Failed to get prediction from model'}) }
    })
})
```

Why each detail:

- **`upload.single('image')`** — accepts exactly one file part named `image`; anything else in the form is
  text fields. `dest: 'image/'` streams to disk (memoryStorage would buffer whole files in RAM — DoS vector).
  The upload directory ships with the repo via `image/.gitkeep` — multer does **not** create directories.
- **`spawn` vs `exec` vs `fork`** — spawn takes an argv array, no shell, streams stdio (right choice for a
  long-running program with binary-safe args). exec buffers everything, uses a shell (injection risk,
  maxBuffer limits). fork is for Node child modules. Never interpolate user input into a shell string.
- **`'close'` vs `'exit'`** — `exit` fires when the process ends but stdio may still be in flight; `close`
  fires after streams are drained, so parsing in `close` guarantees we have all stdout. On spawn *failure*
  (python not found), `error` fires and `close` may or may not — hence the `responded` guard against
  double-response.
- **Accumulate stdout** — a `data` event is "a chunk arrived", not "the message arrived". Large payloads
  arrive in multiple chunks; parsing per chunk crashes. This class of bug is extremely common and we found
  it in the original code.
- **Status semantics** — 400 (client's fault: no file), 500 (server-side failure), 200 with the prediction
  object whose keys are the very strings passed as argv (`plant_name`, `disease_category`, `disease_name`).

### The IPC contract with Python (worth writing on a whiteboard)

```
IN  (argv):  <uploaded-file-path> <plant_name> <disease_category> <disease_name>
OUT (stdout): exactly one line of JSON, e.g. {"plant_name":"Lettuce","disease_category":"Healthy","disease_name":""}
ERR (stderr): free-form logs
SIDE-EFFECT:  python deletes the uploaded file when finished
ON FAILURE:   exception caught in __main__ -> prints {"<key>":"-", ...} or crashes -> 500 at Node
```

Everything else on stdout (TensorFlow logs, Keras progress bars) corrupts this contract — which is exactly
what we fixed by importing with `TF_CPP_MIN_LOG_LEVEL=3` and calling `model.predict(images, verbose=0)`.

### Dependencies (post-cleanup)

| Package | Version | Note |
|---|---|---|
| express | ^4.21.2 (installs 4.22.x) | bumped from 4.16.4 (qs/body-parser CVE fixes) |
| multer | ^1.4.5-lts.2 | bumped from 1.4.2 (DoS CVE-2025-7338) |
| morgan | ~1.12.1 | log-forging fixes |
| debug | ~4.4.0 | was 2.6.9 |
| cookie-parser | ~1.4.7 | |
| nodemon (dev) | ^3.1.10 | was wrongly a production dependency at 2.x |

Removed: `jpeg-autorotate` (its usage was commented out). `npm audit` went from **18 vulnerabilities to 3**
(the remaining three are `braces`/`chokidar` inside dev-only nodemon — no fixed release exists upstream yet).

### Firebase misconception (read this twice)

`firebase.json` configures **Firebase Hosting for the static `public/` folder only** (the one-page site).
The Express API is **not** deployed by this config — Firebase Hosting serves files and rewrites, it does
not run your Node server. In production the API must run on a VM/container (Cloud Run, EC2, a Raspberry Pi,
anything) and the static page is optional. Saying "Firebase hosts my API" is a red flag; saying "Firebase
serves the static page while the Express server runs separately" is the correct story.

---

## 8. Deep dive: ML pipeline and model

### Preprocessing (`scripts/image_preprocess.py`) — exact algorithm

1. `Image.open(path).convert('RGB')` — normalize any input (grayscale, PNG+alpha) to 3 channels.
2. `rotate_image`: 0, 90, 180, 270 degrees — handles wrong EXIF orientation AND doubles as test-time
   augmentation. (The original repo also had a commented-out `jpeg-autorotate` dependency — the rotation
   TTA made it unnecessary.)
3. `image_resize`: scale so the **shorter side = 256** (256.0 exact), longer side = `256 * ratio`, floored
   to int (PIL requires ints). Aspect ratio preserved. Why 256? Larger than the 224 input so crops have
   context margin — the classic 256->224 "train/test crop" scheme from AlexNet-era preprocessing.
4. `image_crop`: upper-left, upper-right, lower-left, lower-right, central — all exactly 224x224 (guaranteed
   since min side >= 224... actually == 256 >= 224).
5. `get_random_images(central)`: one random enhancement — flip horizontal, flip vertical, brightness,
   contrast, sharpness, or color — factor `uniform(0, 2)` rounded to 0.1 (both darker AND brighter possible).
6. Totals: 4 rotations x (5 crops + 1 augmented central) = **24 tensors** per photo.

Spot-the-bug trivia (shows deep reading): `get_random_images` uses `n_functions = 5` while the function
table has **6** entries — the second random image can never select index 5 (color). Harmless today (the
second variant is only used in the training-style path), but exactly the kind of detail an interviewer
loves hearing you found.

### Inference and postprocessing

```python
predictions = model.predict(images, verbose=0)     # dict: name -> (24, n_classes)
pred1 = np.argmax(predictions['plant_name'], axis=-1)   # etc. for each head
m1 = max(set(pred1), key=pred1.count)              # majority vote = statistical mode
label = num_to_label[0].get(str(m1), '-')          # index -> human-readable label
```

- `argmax(axis=-1)` picks the top class **per crop**; the majority vote across 24 crops is
  **test-time augmentation (TTA)**: it makes the prediction robust to where the lesion sits in the frame and
  to rotation.
- Tie behavior: `max(set(...), key=count)` — ties broken by set iteration order; with 24 inputs this is rare,
  but the honest answer is "ties are unspecified; a cleaner implementation would use a deterministic mode
  plus a confidence = votes/24."
- **No confidence threshold / OOD detection**: a photo of a cat still yields a confident-looking plant label
  because softmax always normalizes. The right fix: report `votes/24` as confidence, and reject below a
  threshold (see Q&A).
- Label mapping: `data/num_to_label_file.json` is a list of 3 dicts (plants, categories, diseases) keyed by
  *string* class index; missing entries map to `'-'` (the script's all-purpose "unknown" marker).

### The model artifact

- Format: Keras **SavedModel directory** (`saved_model.pb` + `variables/variables.index` + sharded data).
  Saved by a pre-TF-2.5 Keras (TensorFlow itself warns: "SavedModel saved prior to TF 2.5 detected"), loads
  cleanly in TF 2.10.1.
- Architecture: ImageNet-pretrained **MobileNet V1** kept *including* its 1000-class `conv_preds` top; three
  small Dense heads (`plant_name` 10, `disease_category` 4, `disease_name` 14) hang off the 1000-dim logits.
  ~4.28M params, ~51 MB on disk.
- **The metadata typo**: the Dense layer for disease category is literally named `disease_catetgory`.
  Keras layer names become output keys in some APIs; meanwhile `model.predict` in TF 2.10 returns a dict
  keyed by the (correctly spelled) output signature names. The pipeline now resolves heads robustly:
  exact dict-key match first, `difflib` close-match fallback — works regardless of which convention a
  future TF version or re-saved model uses.

### Why TF 2.10.1 specifically

TensorFlow dropped **native Windows** support after 2.10 — everything newer needs WSL2. The project pins
`tensorflow==2.10.1` (with `numpy<2`, since TF predates NumPy 2's ABI break) in `Backend/requirements.txt`
so the documented setup works on the repo's target platform. On Linux you may use any modern TF, but must
re-verify the predict-dict key behavior.

---

## 9. Design decisions and trade-offs

The format here is Question -> Strong answer -> Honest caveat. Interviewers trust candidates who
volunteer the caveats.

**Q: Why run the model on a server instead of on the phone (TFLite)?**
A: Server-side inference decouples model updates from app releases — retraining and swapping the model
requires zero app-store activity. It also keeps the APK tiny (5.6 MB debug; the model is 51 MB) and works
identically on low-end phones.
Caveat: it requires connectivity, adds a network round-trip and a server to operate/secrure, and every
request pays a ~15-20 s latency. A TFLite int8 version (~5-15 MB) would run offline in <1 s and keep photos
on-device (privacy), at the cost of shipping updates through the store. A great answer names both and says
which you would pick for v2 and why (offline-first + privacy argues for on-device; fast model iteration
argues for server).

**Q: Why three separate output heads instead of one classifier?**
A: The labels are hierarchical: plant (10), disease category (4), disease (14). One 14-way head loses the
plant and category context; a single joint 10x4x14 head would be extremely sparse. Multi-task heads share
the backbone (regularization, fewer params, one forward pass) and give partial credit — even when the
disease is wrong, plant and category are often right, which is better UX.
Caveat: the heads are independent — nothing enforces consistency, so a "Healthy" category can co-occur with
"Mosaic Virus". The clean fix is post-processing that masks invalid (category, disease) combos, or a
structured/hierarchical loss during training.

**Q: Why spawn a Python process per request instead of loading the model inside Node or using TF Serving?**
A: Pragmatism: the team knew Keras/TF, Python owns the ML ecosystem, and the spawn contract (argv in /
JSON line out) is trivially debuggable with `python script.py img.jpg ...` from a terminal. Node stays the
web tier.
Caveat: model load is ~6.6 s of the ~15-20 s request, and every concurrent request forks a ~1-2 GB TF
process — a few dozen concurrent users would OOM a small server. The production fix is a persistent worker
(FastAPI/Flask with the model loaded once, Node calls it over HTTP) or TF Serving with batching. This is
exactly the "how would you scale it" answer.

**Q: Why multipart/form-data and not base64 in JSON?**
A: Multipart transmits raw binary; base64 inflates payloads by ~33% and forces the server to decode.
Also Retrofit's @Multipart + multer's single() map naturally onto each other, and the exact field name
("image") forms a simple contract.

**Q: Why 24 crops + majority voting (TTA)?**
A: A lesion may occupy only part of the frame; corner + center crops at 224 give the network several
"views" at ImageNet's native scale. Voting over views (and rotations) suppresses crop-specific noise.
Caveat: it multiplies compute 24x for a small accuracy gain — cheap on a server, unaffordable on-device.
If you keep it, report vote share as confidence.

**Q: Why Kotlin coroutines over RxJava or raw threads?**
A: Coroutines are the Kotlin-first option: `suspend fun` makes async code linear, Retrofit supports suspend
natively, structured concurrency (scope + SupervisorJob + Dispatchers.Main) replaces callback hell.
Threads would require manual lifecycle care; RxJava is heavier for a simple request-response.

**Q: Why CameraX over the old Camera API or the abandoned CameraKit?**
A: CameraX is lifecycle-aware (`bindToLifecycle`), works across 90%+ of devices via camera2 underneath,
and its use-case model (Preview + ImageCapture) maps directly to the app's needs. The repo originally used
CameraKit (jcenter-only, abandoned) — we removed that dependency and the jcenter repo entirely.

**Q: Why Express and not Firebase Functions / a monolith Python server?**
A: Express is minimal, well-known, and lets the web tier stay stateless. Firebase Functions has cold
starts, limited runtimes (no easy TF+Keras on the same function), and this workload (15-20 s CPU) exceeds
comfortable function timeouts. A pure-Python server would work too — keeping Node was a team-skill choice,
and the IPC boundary is thin.

**Q: Why an ImageNet-pretrained MobileNet and not a bigger net (ResNet/EfficientNet)?**
A: MobileNet V1 is ~4.3M params / ~51 MB, fast on CPU, and pretrained features transfer well to leaf
textures. For 10x4x14 small heads, capacity is rarely the bottleneck — data quantity is.
Caveat: the repo does not include training code or eval metrics; a fair answer is "MobileNet was a good
speed/accuracy trade-off for mobile-era deployment; I'd benchmark against EfficientNet-B0 given more data."

---

## 10. Bugs found and fixed — war stories

Practice 2-3 of these as STAR stories. Each maps to a real commit in the repo history
(see `git log`: 38045f3, cd6429d, 4399c2e, 7eb02d7).

### Story 1 — The app that could never have worked (the best one)

**Situation:** After the backend returned correct JSON via a direct HTTP client, the Android app still
always showed "Some Error Occurred" on every submission.
**Task:** Find where a working API and a working app disagree.
**Action:** Read the stack trace path: Retrofit -> OkHttp -> SocketTimeoutException. The server takes
15-20 s (it loads a 51 MB model per request); OkHttp's **default read timeout is 10 s**. The client gave up
before the server ever wrote a byte of response. Built an explicit `OkHttpClient` with 30/60/30 s timeouts
on both Retrofit builders.
**Result:** App talks to the server. **Lesson:** "works via curl" is not "works in the app" — client
defaults matter; test the real client. (commit 7eb02d7)

### Story 2 — A corrupted stdout contract

**Situation:** The endpoint intermittently 500'd or crashed the server depending on machine.
**Task:** Make process IPC reliable.
**Action:** Found two defects: the original code called `JSON.parse` on *every* stdout chunk (streams
deliver partial data — crash on any split), and Keras printed a progress bar (`1/1 [====...`) onto stdout,
prepending garbage to the JSON. Fixed by accumulating chunks and parsing once on `close`, plus
`model.predict(..., verbose=0)` and `TF_CPP_MIN_LOG_LEVEL=3` to keep stdout pure.
**Result:** One-line JSON contract guaranteed. **Lesson:** stdout is a *stream*, not a message queue —
define a wire format and frame it (here: single JSON line, logs to stderr). (commit cd6429d)

### Story 3 — A typo frozen inside a 51 MB artifact

**Situation:** Model loaded fine, but predictions silently returned "-" for everything on a fresh machine.
**Task:** Trace why inference produced no labels.
**Action:** First fixed a **hardcoded absolute path** (`/home/aayussss2101/Desktop/Leaf Disease/...`) in
the script — the app only ever worked on the original author's laptop. Then discovered the model's Keras
metadata contains a misspelled layer name (`disease_catetgory`), while `predict()` keys its dict by the
correctly spelled signature names — naive key lookups crash on one or the other depending on TF version.
Implemented robust head resolution (exact key first, close-match fallback) and verified with a real
lettuce photo -> `{"plant_name":"Lettuce","disease_category":"Healthy","disease_name":""}`.
**Lesson:** Never trust names inside binary artifacts; verify contracts against the loaded object at
runtime. And: check in code for machine-specific paths. (commits 38045f3, cd6429d)

### Story 4 — Invisible camera buttons (UI z-order)

**Situation:** Camera screen rendered, but the capture/flash/switch buttons were unclickable.
**Action:** In Android, later children of a ViewGroup draw on top. `PreviewView` was declared *after* the
buttons in `camera_view.xml`, so the full-screen preview covered them and ate touches. Moved the preview to
be the first child.
**Lesson:** XML declaration order IS z-order; run the app on a device before calling a layout done.

### Story 5 — Crashes on empty states and permission races

**Situation:** Gallery/camera screens could crash on a fresh device.
**Action:** Two bugs: (a) `sortImageUri()` indexed `list[0]` immediately — an empty device gallery or a
storage permission not yet granted = `ArrayIndexOutOfBoundsException`; added an empty-list guard in both
ViewModels. (b) `CameraActivity` called `getImages()` twice — once inside an `if empty` block and once
unconditionally — doubling every image in the list; removed the duplicate call.
**Lesson:** Test the empty state, and remember runtime permission grants are asynchronous (the query runs
before the grant).

### Story 6 — The repo that could not build on Windows

**Situation:** `./gradlew assembleDebug` failed for any Windows user.
**Action:** Three separate causes: (1) the Unix `gradlew` script had been committed as `gradlew.txt`
(renamed on upload; also lost its exec bit — fixed with `git mv` + `update-index --chmod=+x`); (2) the
project pinned `buildToolsVersion "30.0.3"`, which **was never released for Windows** (macOS/Linux only) —
pinned 30.0.1 instead; (3) `jcenter()` was dead (sunset) and the only jcenter-hosted dependency
(CameraKit) was unused — removed both.
**Result:** Fresh clone + JDK 11 + SDK 30 builds in ~4 s warm, APK 5.6 MB, unit tests pass.
**Lesson:** A repo is only "working" if it builds from a fresh clone on a different OS. (commits 38045f3, 4399c2e)

### Story 7 — Dependency hygiene as engineering

**Action:** `npm audit` reported 18 vulnerabilities. Bumped multer (DoS CVE-2025-7338), Express 4.16->4.22
(qs/body-parser CVEs), morgan (log forging), moved nodemon to devDependencies, and deleted the unused
`jpeg-autorotate` dependency entirely. Final state: 3 findings, all in dev-only nodemon chain
(braces/chokidar) with no upstream fix released yet.
**Lesson:** Dependencies are attack surface; production vs dev placement matters; document what you cannot
fix and why.

### Minor fixes worth mentioning if asked

- `routes/index.js` called `res.render('index')` with **no view engine configured** — latent 500; now
  `res.sendFile`s the static page.
- Uploads directory `Backend/image/` was not in the repo — multer 500'd on a fresh clone (multer does not
  create directories); shipped `image/.gitkeep`.
- Non-RGB uploads (grayscale/PNG+alpha) crashed the model input — added `.convert('RGB')`.
- Launcher activity lacked `android:exported="true"` — mandatory for Android 12+ installs when targeting 31.
- Added `Backend/requirements.txt` (tensorflow==2.10.1, numpy<2, pillow) for reproducibility.

---

## 11. Android interview Q&A

**Q: Difference between compileSdk, targetSdk and minSdk?**
A: compileSdk = which Android API the *compiler* builds against (30 here). targetSdk = which behavior
contract the app *opts into* at runtime (30 — so no forced scoped-storage/12+ export rules). minSdk = lowest
version supported (23 = Android 6.0). This app supports 6.0 through the newest devices.

**Q: What is the AndroidManifest and what does this app declare?**
A: The app's contract with the OS: package id, components (4 activities), permissions (INTERNET, CAMERA,
READ/WRITE_EXTERNAL_STORAGE), hardware feature `camera.any` required, `usesCleartextTraffic` (needed for
the plain-HTTP LAN server), and the launcher activity with `exported=true`.

**Q: Why was `android:exported` needed?**
A: On Android 12+, any component with an intent filter MUST declare its export state explicitly —
installation fails otherwise once you target API 31. The launcher activity must be exported (the system
starts it). Others stay non-exported (default false) so other apps cannot launch them.

**Q: How does CameraX bind to lifecycle?**
A: `cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)` — CameraX opens/closes
the camera in sync with the LifecycleOwner, so no manual onStart/onStop handling and no leaked camera in
background. The old code's manual onStart/onStop was part of the abandoned CameraKit integration.

**Q: Explain coroutines usage here — scope, job, dispatchers.**
A: `CoroutineScope(SupervisorJob())` — failures of one child don't cancel the rest. `activityScope.launch`
starts the request (Retrofit suspend executes on OkHttp threads); `launch(Dispatchers.Main)` hops back to
the UI thread to update the progress bar and show the popup. SupervisorJob vs plain Job and why we don't
use GlobalScope (lifetime tied to nothing) are standard follow-ups.

**Q: What is `runBlocking` and why is it a smell here?**
A: It blocks the current (main) thread until the coroutine completes. `MainActivity` uses it for the demo
upload: it doesn't throw NetworkOnMainThreadException (the actual HTTP runs on OkHttp's own threads) but
the UI freezes for ~15-20 s. Correct pattern = the one used in SubmitActivity.

**Q: How does Retrofit map JSON to Kotlin objects?**
A: `GsonConverterFactory` converts the response body into `PlantData`; `@SerializedName` binds
`plant_name` -> `plantName` etc. `diseaseName` is `String?` because healthy plants may carry an empty
label. The suspend function wraps OkHttp's async call so it reads sequentially.

**Q: What is multipart/form-data?**
A: An HTTP body format of parts delimited by a `boundary` string; each part has headers (Content-Disposition
with name/filename, Content-Type) and a payload. Lets one request carry binary files and text fields.
Retrofit's `@Multipart` + `@Part` builds it; multer parses it server-side. Alternative — base64-in-JSON —
costs ~33% size overhead.

**Q: What are OkHttp's default timeouts, and how did they bite you?**
A: connect/read/write = 10 s each. The model route needs 15-20 s, so every request died with
SocketTimeoutException at 10 s — the app "never worked" despite a healthy server. Fix: explicit
`OkHttpClient.Builder()` with 30/60/30 s. Classic real-world debugging story.

**Q: How does the gallery grouping work?**
A: MediaStore query for (_ID, DATE_TAKEN) sorted DESC -> list of (uri, timestamp) -> `sortImageUri` buckets
by calendar boundaries (this week, last week, months, years) using a binary search (`getLast`) per boundary.
ViewModels cache results across config changes.

**Q: What is a RecyclerView.ItemDecoration?**
A: A hook to draw around/behind/over items and offset their layout. The sticky-header decoration overrides
`onDrawOver` (canvas ABOVE items), creates the header ViewHolder manually (`createViewHolder` +
`bindViewHolder`), and pins/translates it as sections scroll. `onDraw` would draw *behind* items.

**Q: Why two nested RecyclerViews (sections -> grid)?**
A: Pragmatism: outer list = sections (header/inner-list view types), inner = 4-column grids with Glide.
Caveat to volunteer: nested scrollables have measurement costs and complicate shared items; the cleaner v2
is one flat grid where headers span all columns via `GridLayoutManager.SpanSizeLookup`.

**Q: What does Glide do here and why not just setImageURI?**
A: Content-URI thumbnails in a scrolling grid — Glide does async load, memory/disk caching, downsampling,
and view recycling correctness (cancel on detach). setImageURI would decode on the main thread.

**Q: Explain the Activity Result API usage.**
A: `registerForActivityResult(ActivityResultContracts.TakePicture())` / `GetContent()` return launcher
objects; calling `launch(...)` starts the flow and the callback receives the result — replacing
startActivityForResult + requestCode switch blocks. Type-safe and lifecycle-safe.

**Q: Why `Uri.fromFile` works here but is usually banned?**
A: `file://` Uris exposed to *other apps* trigger `FileUriExposedException` on API 24+ — the normal fix is
a FileProvider. Here the temp file lives in the app's own external dir and is handed to the *system camera*
via TakePicture... the app also relaxes StrictMode VmPolicy (see `MainActivity`), which is why it runs.
A cleaner v2 uses FileProvider; worth admitting.

**Q: What is `usesCleartextTraffic="true"`?**
A: Allows plain HTTP (no TLS) network calls. Required because the backend URL is `http://192.168.1.8:3000/`.
In production this is a security hole — the fix is HTTPS behind a reverse proxy, then remove the flag.

**Q: What is scoped storage and does this app comply?**
A: Android 10+ limits shared-storage access. The app: writes only to its own app-specific dirs (no
permission needed), reads the shared gallery via MediaStore with READ_EXTERNAL_STORAGE (the sanctioned
path). The declared WRITE_EXTERNAL_STORAGE + `tools:ignore="ScopedStorage"` is legacy-era practice to
retire when bumping targetSdk to 30+ enforcement.

**Q: kotlin-android-extensions vs ViewBinding?**
A: Synthetics (kotlinx.android.synthetic imports in CameraActivity) were deprecated in Kotlin 1.8 and
removed later; ViewBinding is the supported replacement — a generated binding class per layout, null-safe,
no findViewById. Migration is a known roadmap item for this codebase.

**Q: What would R8/ProGuard do for the release build?**
A: Currently `minifyEnabled false`. Enabling R8 shrinks/obfuscates and strips unused code (APK size,
reverse-engineering resistance), at the cost of keeping rules for reflection-based libs — notably
Gson model classes need `@Keep` or proguard rules, or serialization breaks silently.

---

## 12. Backend interview Q&A

**Q: Walk through the Express middleware stack of this app.**
A: morgan (request logging) -> express.json / urlencoded (body parsers; no-ops for multipart) ->
cookieParser -> express.static(public) -> routers. For POST /image, multer's `.single('image')` runs as
route-level middleware BEFORE the handler.

**Q: What does multer do, and what are its storage options?**
A: Multipart parsing middleware. `.single(fieldname)` accepts one file part into `req.file`.
`dest: 'image/'` = disk storage with random hex filenames (streamed to disk — not buffered in RAM).
`memoryStorage` would keep bytes in memory (DoS risk on big uploads). Multer does NOT create the target
directory — the repo ships `image/.gitkeep` for that reason.

**Q: spawn vs exec vs fork?**
A: `spawn(program, argvArray)` — no shell, streams, right for long-running programs (and safe: no string
interpolation). `exec(cmdString)` — runs via a shell, buffers output up to maxBuffer (injection-prone).
`fork(module)` — spawns a Node child with an IPC channel. The app uses spawn because the worker is Python.

**Q: Why 'close' rather than 'exit'?**
A: `exit` fires when the process terminates, but its stdio pipes may still hold data. `close` fires when the
process ended AND the streams are drained — so the final stdout (the JSON) is guaranteed collected. On
spawn *failure*, `error` fires and `close` may too — the code guards against double-responding with a
`responded` flag.

**Q: How is the response kept well-formed under failures?**
A: All exits funnel through one `respond(status, body)` closure: 400 for missing upload (client error),
500 for spawn error or unparsable worker output, 200 with the prediction JSON otherwise. The worker itself
has its own try/except that prints all-"-" labels, and Node treats unparsable output as 500.

**Q: Is spawning a subprocess with a user-influenced path a command-injection risk?**
A: No shell is involved — the path is passed as a single argv element (multer's generated hex temp name,
not the user's filename), so no metacharacters can re-trigger a shell. The injection class applies when
people build `exec(\`python script.py ${userInput}\`)`. Remaining honest risks: unbounded upload size, no
MIME allowlist, and reliance on `python` from PATH (see Security section).

**Q: Why JSON over stdout and not an HTTP call between Node and Python?**
A: Zero extra infrastructure; process boundary is the sandbox (a Python crash can't take down Node);
debuggable by hand (`python scripts/image_preprocess.py img.jpg plant_name ...`). Cost: process startup +
model load per request, and a fragile stdout contract that must be kept pure — the exact bug we fixed
(Keras progress bar). v2 should be a persistent worker service.

**Q: What is the role of `constants.js`?**
A: Single source of truth for the three label keys (`plant_name`, `disease_category`, `disease_name`) —
they are passed to the Python script as argv, used to index the prediction dict, and become the JSON
response keys the app's Gson model expects. One rename here silently changes the whole contract —
deliberate coupling worth documenting.

**Q: What does `bin/www`'s normalizePort/error handling do?**
A: Accepts port as env var or 3000; returns number, or the raw string (named pipe), or false. On EADDRINUSE
it prints "already in use" and exits 1; on EACCES "requires elevated privileges". Boilerplate from
express-generator — knowing it exists signals you actually read the repo.

**Q: Where would you add rate limiting, and how?**
A: `express-rate-limit` middleware in `app.js` before the routers (e.g., 20 requests/minute/IP for
/image), plus a reverse proxy (nginx) with connection caps — important because each request costs a
~15-20 s CPU-bound worker.

---

## 13. ML interview Q&A

**Q: Why 224x224 input?**
A: The ImageNet-standard input size the backbone (MobileNet V1) was pretrained at; keeping it means the
pretrained filters work in-distribution. 256->224 crop scheme mirrors classic ImageNet preprocessing.

**Q: Why resize to 256 and crop, instead of squashing to 224?**
A: Aspect-ratio squash distorts leaf shape/texture; short-side-256 + 224 crops preserve geometry and give
four shifted views + center. Crops double as TTA at inference here.

**Q: What augmentations are used and why?**
A: Horizontal/vertical flips, brightness, contrast, sharpness, color — factor uniform(0,2) so both
directions occur; plus rotation (0/90/180/270). Leaves photograph at any orientation/zoom/lighting, and
these transformations don't change the label — label-preserving augmentation is exactly what you want.

**Q: Why majority voting over 24 predictions instead of averaging probabilities?**
A: Voting (mode of argmaxes) is robust to outlier crops and simple to reason about; probability averaging
would be smoother and retains confidence — the better v2. With 24 views, report `votes/24` as a confidence
score and threshold low-confidence outputs.

**Q: Explain multi-task learning benefits here.**
A: One shared backbone serves three related classification tasks: parameter efficiency (heads are ~10-14k
params each), regularization (multi-task signal reduces overfitting on a small dataset), one forward pass
for all outputs. Risk: negative transfer if heads conflict (rare here since all tasks are image-derived).

**Q: Why does the model have an ImageNet 1000-class layer if it's classifying leaves?**
A: The original author kept MobileNet's `conv_preds` (1000) and attached the three heads to its 1000-dim
output — the ImageNet logits act as a feature layer. It works (verified end to end) but is unusual;
cutting the top and using the 1024-dim pooling output would be the cleaner transfer-learning setup.

**Q: What is a SavedModel? Compare with .h5 and TFLite.**
A: SavedModel = directory (saved_model.pb graph + variables/ weights), language-agnostic, servable (TF
Serving) and the format used here. .h5 = single-file Keras weights+architecture (legacy). .tflite =
FlatBuffers for on-device interpreters, often quantized. Conversion pipeline: SavedModel -> TFLite
converter (int8/fp16) -> ~5-15 MB for this model.

**Q: How would you evaluate this model properly?**
A: Per-head metrics: accuracy, macro-F1 (classes are imbalanced — macro weights rare diseases equally),
confusion matrices (which diseases get conflated), and hierarchical consistency rate (does category agree
with disease?). Hold out per-plant, report confidence (vote share) calibration. The repo currently has no
eval script or stored metrics — say that honestly and describe what you would add.

**Q: What happens if you send a photo of a cat?**
A: The softmax still assigns probabilities — you'd get a confident-looking "plant" label. There is no
out-of-distribution rejection. Fixes: confidence threshold on vote share, an OOD/background class, or a
two-stage "is this a leaf?" detector. Great question to raise proactively.

**Q: Why pin tensorflow==2.10.1 and numpy<2?**
A: TF >2.10 dropped native Windows (needs WSL2), and TF <2.16 is incompatible with NumPy 2's ABI —
unpinned installs break confusingly. requirements.txt makes the stack reproducible; on Linux you can
move to modern TF after re-verifying the predict-dict key behavior.

**Q: Where does training code live?**
A: It doesn't — the repo ships the trained artifact + label maps, not the training script, dataset, or
metrics. Honest answer + roadmap: publish the training notebook, dataset source/license, split definition,
metrics per head, and a retraining pipeline (see System Design).

**Q: How would you update the model in production?**
A: Server-side advantage: drop a new `plant_leaf_model/` directory and restart workers — no app release.
Add: versioned model dirs + a manifest endpoint, canary traffic (1%), compare per-head metrics on a fixed
golden set, one-click rollback. Never hot-swap silently.

---

## 14. System design and scaling Q&A

**Q: What breaks first at 100 concurrent users?**
A: RAM. Each request spawns a Python process that loads a ~51 MB model with TF runtime overhead
(~1-2 GB RSS each); ~100 concurrent spawns OOM a typical 8-16 GB host, plus 100x CPU contention
(each request needs 15-20 s of CPU). Node itself is fine — the ML fan-out is the bottleneck.

**Q: Design the v2 architecture.**
A:
1. **Persistent model service**: FastAPI/Flask (or TF Serving) loading the model once at startup; Node
   calls it over HTTP/gRPC. Cuts per-request latency from ~15-20 s to ~1-2 s and RAM becomes O(workers)
   not O(requests).
2. **Queue + worker pool** (Redis + BullMQ / Celery): uploads enqueue; N GPU/CPU workers pull jobs;
   natural rate limiting, retries, and backpressure. Client can poll or get a push.
3. **Horizontal scale**: Node tier is stateless — scale behind a load balancer. Workers scale by
   queue depth (autoscaling signal).
4. **Caching**: hash the uploaded image bytes; memoize identical submissions.
5. **Batching**: TF Serving batch requests for GPU efficiency.
6. **Monitoring**: p95 latency, queue depth, worker RSS, model-version labels on responses, error rates.

**Q: How do you deploy this today, cheaply?**
A: Docker Compose: `node` (API), `model-server` (Python + TF, model baked into the image), optional
`nginx` for TLS termination and static hosting. A 4 GB / 2 vCPU VM comfortably serves a small user base
with the persistent-worker fix. The current per-request-spawn design would need a big instance and still
waste resources.

**Q: What about on-device inference (TFLite)?**
A: Convert with int8 post-training quantization -> roughly 5-15 MB, sub-second on modern phones, offline,
private. Trade-offs: app size, per-device variance, no central model updates, retraining workflow tied to
releases. A hybrid — on-device first, server fallback — is the strongest answer.

**Q: Where does the uploaded image go? Privacy?**
A: Temp file `Backend/image/<hex>` -> deleted by the worker after inference (even on the failure path
in the script's `__main__`). Missing pieces worth volunteering: cleanup of files orphaned by mid-flight
crashes, a retention cap, and consent/anonymization if uploads are ever retained for retraining
(data-collection policy/PII angle).

**Q: How do you version this system?**
A: API version in the path (/v1/image), response includes model_version; the three label keys are a
contract shared by constants.js, the Python argv, and the app's @SerializedName — change them in lockstep
with a migration (new keys + deprecation window). Model dirs versioned on disk; manifest maps "latest".

**Q: What is your DR/rollback story?**
A: Model rollback = previous model dir + restart (server-side advantage). API rollback = previous
container tag. App = Play Store staged rollout. Data loss is minimal (stateless), but a shared
uploads/queue store must be persistent if you add async processing.

---

## 15. Security Q&A

**Q: Enumerate this project's security weaknesses honestly.**
A: (Volunteering this list is a strength.)
1. **Plain HTTP** (`usesCleartextTraffic`, `http://192.168.1.8:3000/`) — traffic and the leaf photo are
   sniffable/alterable on the LAN. Fix: TLS via nginx/Caddy + ACME, then remove the cleartext flag.
2. **No authentication** — anyone reaching the API can spam predictions (each costs ~15-20 s CPU = cheap
   DoS). Fix: per-device token/JWT + rate limiting.
3. **No upload limits/validation** — multer accepts any size/type; a 2 GB upload exhausts disk/RAM.
   Fix: `limits: {fileSize: 10MB}`, MIME allowlist, magic-byte sniffing (not extension trust).
4. **`python` resolved from PATH** — a hostile binary earlier in PATH wins. Pin an absolute interpreter
   path in prod.
5. **Verbose error leakage risk** — we deliberately return generic messages; keep stack traces in logs.
6. **No CORS policy needed yet** (mobile client), but a web client would need a strict policy.

**Q: Is the spawn call injectable?**
A: No — argv array, no shell, and the file path is multer's generated hex name (user controls bytes, not
the path). Command injection would require building a shell command string from input. The *risk* class
to mention: unbounded work per request (DoS), not injection.

**Q: Android-side security notes?**
A: `android:exported` now correctly set (launcher only). No secrets in the app (server has none either).
WRITE_EXTERNAL_STORAGE is over-broad for what the app does — remove when bumping targetSdk. The FileUri
exposure workaround (StrictMode relaxation) should be replaced by a FileProvider.

**Q: What about the known-CVE dependency situation?**
A: Audited: 18 -> 3 findings. Remaining three (braces/chokidar in nodemon's chain) are dev-only with no
fixed release upstream; documented, watched, and not shipped to production. `npm ci` (lockfile-exact
installs) in CI prevents silent drift.

---

## 16. Testing and reliability Q&A

**Q: What tests exist, and what would you add first?**
A: Today: one trivial unit test (2+2) and one instrumented package check — placeholder level. This
debugging effort added *system-level* verification: real-image end-to-end prediction via the HTTP route,
400/500 failure-path checks, and a reproducible `assembleDebug` + unit-test build. First additions, ranked:
1. **Unit**: `sortImageUri`/`getLast` boundary logic (pure functions — empty list, boundary timestamps,
   single image, leap weeks). Highest value per line of test code in the repo.
2. **Contract**: the JSON schema of /image (keys, types, '-' fallbacks) — shared spec between Node and app.
3. **Integration**: Express with supertest (400 path, spawn-failure path with a stubbed script);
   Python preprocessing with pytest on fixture images (crop sizes, RGB conversion, vote counting).
4. **Android**: MockWebServer for Retrofit happy/timeout/500 paths; UI test of the popup flow.

**Q: How do you run this project from scratch? (Be ready to state it in 30 seconds.)**
A: Backend — `cd Backend && npm install`, `pip install -r requirements.txt` (Python with TF on PATH),
`node ./bin/www` (port 3000). Android — open `Android/` in Android Studio, set your machine's LAN IP in
`strings.xml`, run on a device on the same network. Sanity check without the app:
`curl -F image=@leaf.jpg http://localhost:3000/image`.

**Q: What did "verifying it works" actually catch that reading code didn't?**
A: Three invisible-from-reading bugs: Keras writing its progress bar into the stdout contract;
the model-metadata typo vs signature keys; and the OkHttp 10 s timeout making the entire app fail despite
a healthy server. Lesson to state in interviews: end-to-end verification against the real artifact is
the only proof; every one of these looked "fine" in the source.

**Q: CI/CD for this repo?**
A: Two jobs: (1) Backend — `npm ci`, `pip install`, supertest+pytest, a smoke `node ./bin/www` + curl.
(2) Android — gradle wrapper assembleDebug + unit tests on a Linux runner with pre-cached SDK.
Nightly `npm audit`/`pip-audit`. Fresh-clone builds on more than one OS (the Windows-only build-tools bug
is the cautionary tale).

---

## 17. The questions students forget to prepare

*These are the ones that end project rounds. Read this section twice.*

**1. "What is your model's accuracy?"**
The repo has no eval metrics or training code. NEVER invent a number. Strong answer:
"There's no stored eval in the repo, which I consider a gap — if I add evaluation, I'd report per-head
accuracy and macro-F1 (classes are imbalanced), confusion matrices per disease, and a hierarchical
consistency rate between category and disease heads, on a plant-stratified holdout. Qualitatively, I
verified correctness end to end: a real lettuce photo returns plant 'Lettuce', category 'Healthy'."

**2. "Where did the data come from? How many images?"**
Know your team's true answer. What you CAN say from the repo: 10 plant classes, 4 categories, 14 diseases
(one label is the empty string for healthy), implying a curated herb/leaf dataset (not PlantVillage — its
classes don't overlap with Basil/Mint/Chives). If you can't state the source, say how you'd find out
(training notebook, dataset card) — never bluff.

**3. "What happens with a photo of a cat / no leaf at all?"**
The model always answers — softmax never says "none of the above". No OOD rejection, no confidence
threshold (vote share is computed but discarded). Volunteer the fix: threshold votes/24, add a
background class, or a two-stage leaf detector.

**4. "Why can the three heads disagree?"**
Independent heads can output "Healthy" + "Mosaic Virus". No hierarchy enforcement at inference. Fix:
post-processing constraint table, or masked/structured training. (Say it before they find it.)

**5. "What's the actual end-to-end latency, and why?"**
~15-20 s per request: ~6.6 s cold model load (per request!), ~1.2 s inference, plus process/IO. Quote the
breakdown — knowing *where* time goes is the impressive part, plus the v2 (persistent worker) that drops
it to ~1-2 s.

**6. "Did the app ever work before you fixed it?"**
Honest, high-credibility answer: no — three independent blockers (hardcoded model path, stdout contract
corruption, OkHttp timeout) meant the system only looked assembled; it was never verified end to end.
Presenting that diagnosis calmly is a strength, not a confession.

**7. "What's incomplete in this project?"**
Have the list ready: camera capture isn't wired to submit; MainActivity is a runBlocking demo screen;
no auth/TLS/rate limits; no model eval or training code; no real test suite; legacy WRITE_EXTERNAL_STORAGE
permission; deprecated synthetics instead of ViewBinding; commented-out bottom-sheet gallery;
hardcoded LAN IP.

**8. "What would you improve first, and in what order?"**
1) Persistent model worker (latency 20x). 2) TLS + token auth + upload limits. 3) Confidence reporting +
OOD threshold. 4) Wire camera capture into submit. 5) Tests for sortImageUri + HTTP contract.
Order rationale: correctness -> safety -> UX -> polish.

**9. "Walk me through the ugliest code you'd refactor."**
GalleryViewModel vs CameraViewModel near-duplicates (one class, injected label strategy); the flat
`MutableList<Any>` alternating header/items in the adapter (sealed types); `runBlocking` in MainActivity;
the `position % 2` view-type trick (replace with typed view models + DiffUtil).

**10. "Why 10 plants / 4 categories / 14 diseases specifically?"**
Say the label structure from the data files, and if you don't know the dataset's provenance, say how
you'd determine it. Know at least: categories are Fungal/Healthy/Viral/Bacterial; healthy plants carry an
empty disease label.

**11. "How does Firebase fit in?"**
It hosts the static public/ page ONLY (firebase.json rewrites to index.html). The Express API runs
separately. Misclaiming "Firebase runs my backend" is a classic tell.

**12. "What does a prediction response actually look like?"**
`{"plant_name":"Lettuce","disease_category":"Healthy","disease_name":""}` — keys from constants.js,
empty disease for healthy, `'-'` values on worker-side exceptions. Quote it verbatim.

**13. "What if two users submit simultaneously?"**
Two Python processes spawn — works (verified), each paying full model load; degrades linearly until RAM
exhaustion. Not a correctness race (isolated temp files via multer hex names) — a capacity cliff.

**14. "What did you personally do?"**
Prepare a first-person, honest split. The debugging narrative is fully defensible solo work: 18
vulnerabilities triaged, 15+ bugs fixed, end-to-end verification built, builds green on Windows.
Attach commits: 38045f3, cd6429d, 4399c2e, 7eb02d7.

**15. "How would you make money / who is the user?"**
Farmers/hobby gardeners/agri-extension workers; freemium with bulk scanning; or B2B: crop-inspection
APIs for greenhouses. Weak answers ignore distribution: rural users -> offline-first matters -> argues for
TFLite.

**16. "Legal/ethical constraints?"**
It is an advisory tool, not an agronomist — misdiagnosis of regulated pathogens has economic impact;
add disclaimers, keep human-in-the-loop, and mind dataset licensing before commercial use.

**17. "What surprised you most?"**
The best material: a working server + working app can still produce a broken product because of a
10-second default; and a typo frozen inside a 51 MB binary artifact outliving its author.

**18. "Estimate the model's memory/compute on a phone."**
MobileNet V1 @224 is ~4.3M params (~17 MB fp32) and ~560M MACs — comfortably real-time on modern
phones after int8 quantization (~5 MB). Back-of-envelope literacy matters.

**19. "What is 'the 24' in the pipeline?"**
4 rotations x (4 corner + 1 center + 1 augmented center) = 24 tensors -> 24 votes per head. The single
most quotable ML number in this project.

**20. "What question do you wish I'd asked?"**
Have one: "How do you keep the stdout IPC contract honest?" — a story connecting protocol design,
cross-language boundaries, and defensive parsing.

---

## 18. Rapid-fire cheat sheet

| Prompt | One-line answer |
|---|---|
| Stack in one breath | Kotlin/AndroidX app -> Retrofit multipart -> Express+multer -> spawn Python -> TF Keras SavedModel -> JSON back |
| Model | MobileNet V1 (ImageNet), 4.28M params, 3 heads: 10/4/14, ~51 MB |
| Preprocess | RGB -> 4 rotations -> short-side-256 resize -> 5x224 crops + 1 augmented center = 24 |
| Postprocess | argmax per head per crop -> majority vote -> label map |
| Latency | ~15-20 s e2e (load 6.6 s + infer 1.2 s dominate); v2 persistent worker ~1-2 s |
| App versions | minSdk 23 / target+compile 30, Kotlin 1.5, AGP 4.2, Gradle 6.7.1, build-tools 30.0.1 |
| Key libs | Retrofit 2.9 + Gson, Glide 4.12, CameraX 1.0.1, coroutines 1.4.1, Express 4.2x, multer 1.4.5-lts.2, TF 2.10.1 |
| Wire format | POST /image multipart field "image" -> 200 `{"plant_name","disease_category","disease_name"}` |
| IPC | argv in / one JSON line out / logs to stderr / worker deletes the temp file |
| Debug best-seller | OkHttp 10 s default read timeout vs 20 s inference — the app never worked |
| Scaled design | FastAPI/TF-Serving worker + Redis queue + TLS proxy + rate limits + model versioning |
| Security gaps | HTTP, no auth, no upload caps, PATH python — each with a named fix |
| The typo | Dense layer `disease_catetgory` inside the SavedModel vs signature keys — robust resolver |
| Audit | npm 18 -> 3 vulnerabilities; the rest are dev-only braces/chokidar with no upstream fix |

## 19. Self-test mock interview

Answer aloud, then check the section in brackets.

1. Walk me through the system from camera shutter to popup. [4]
2. Why three heads and not one classifier? [9]
3. What exactly is in the multipart body your app sends? [6 / 12]
4. Why does a request take 15-20 seconds and how do you get to 1-2? [5 / 14]
5. What is 'close' vs 'exit' on a child process, and why does it matter here? [7 / 12]
6. What does TTA mean in this pipeline, numerically? [8]
7. How does the sticky header actually get drawn? [6 / 11]
8. Your model says "Healthy" but also "Mosaic Virus". Diagnose. [13 / 17.4]
9. A user submits a cat photo. What comes back and why is that bad? [17.3]
10. What is in your model artifact directory and how do you know? [8]
11. Why is there an empty-string disease label? [5 / 17.10]
12. Rate-limit and protect this API without touching the app. [15]
13. What breaks at 100 concurrent uploads, in what order? [14]
14. What does firebase.json actually deploy? [17.11]
15. Why did `assembleDebug` fail on Windows before you fixed it, three separate reasons? [10]
16. What test would you write first in this repo and why? [16]
17. Why TF 2.10.1 and numpy<2, exactly? [8]
18. Where is runBlocking, why is it there, and what replaces it? [6 / 11]
19. Your model is updated weekly. Walk me through safe rollout. [13 / 14]
20. What would you say if I asked "what is your accuracy"? [17.1]

## 20. Day-before checklist and glossary

### Checklist

- [ ] Recite the 30-second pitch without notes.
- [ ] Recite the request lifecycle (Section 4) end to end, including failure modes.
- [ ] Quote from memory: 10/4/14 classes, 24 crops, 4.28M params, ~51 MB, 15-20 s, minSdk 23.
- [ ] Tell Stories 1-3 (Section 10) out loud in STAR form, each under 90 seconds.
- [ ] Name three weaknesses and their fixes before the interviewer asks (Section 15).
- [ ] Have the "no stored accuracy" answer ready, worded calmly and precisely (17.1).
- [ ] Re-read Section 17 one final time — it is the differentiator.

### Glossary (short)

- **TTA** — test-time augmentation: predict on transformed copies (crops/rotations), aggregate (here: vote).
- **Majority vote / mode** — most frequent value; `max(set(x), key=x.count)` computes it here.
- **SavedModel** — TF's portable directory format: graph (saved_model.pb) + variables/.
- **Depthwise-separable conv** — MobileNet's factorized convolution (spatial filter per channel, then
  1x1 mix) — the `conv_dw`/`conv_pw` layer pairs in the summary.
- **Transfer learning** — reusing ImageNet-learned features; here even the 1000-class top is kept as
  a feature layer feeding three small heads.
- **Multi-task learning** — one shared backbone, several supervised heads trained jointly.
- **Macro-F1** — per-class F1 averaged equally; the right headline metric under class imbalance.
- **OOD rejection** — refusing inputs outside training distribution (cat photos).
- **Multipart/form-data** — HTTP format for mixed binary/text bodies, delimited by a boundary.
- **TTA alternative: probability averaging** — averaging softmax vectors instead of voting on argmax.
- **Sticky header** — section header pinned on-screen by drawing over the RecyclerView canvas (onDrawOver).
- **SupervisorJob** — a coroutine Job whose children fail independently.
- **Model-as-a-service boundary** — the Node->Python IPC seam; where latency and reliability are won/lost.

---

*Guide generated from a full working verification of the repository: backend e2e-tested with a real leaf
image (correct prediction), Android assembled and unit-tested from a clean toolchain, model architecture
inspected from the loaded artifact. Cross-reference commit history 38045f3, cd6429d, 4399c2e, 7eb02d7.*

