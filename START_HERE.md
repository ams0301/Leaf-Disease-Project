# START HERE — Understanding Your Own Project (Beginner Friendly)

> Read this BEFORE `INTERVIEW_PREP.md`.
> `INTERVIEW_PREP.md` is the complete reference. THIS file teaches you to actually understand
> and explain the project — every term you might say is defined in plain English first.
>
> How this file works:
> - Part 1 tells you what the project IS (one page, no jargon).
> - Part 2 tells the journey of one photo as a story — this is what you explain in the interview.
> - Part 3 is a dictionary of every term, so you can never be caught out by your own words.
> - Part 4 gives you memorized scripts. Part 5 explains the 7 ideas you must understand deeply.
> - Part 6 lists phrases that will hurt you. Part 7 is flashcards. Part 8 is your study plan.

---

## Part 1 — What this project is (no jargon version)

### One paragraph

You take a photo of a leaf on a phone. The app sends the photo over Wi-Fi to a server (a program running
on a computer). On that server, another program looks at the photo with a "trained brain" — a machine
learning model that has been shown thousands of leaf photos before and learned to recognize patterns.
The model answers three questions: **which plant is this** (out of 10 it knows), **is it healthy or sick**
(fungal / viral / bacterial / healthy), and **if sick, which disease** (out of 14). The server sends the
answer back as a short text message, and the app shows it on screen.

That's the whole project. Everything else is detail.

### Why it is useful (say this if asked "why build it?")

Farmers and gardeners cannot get a plant expert to inspect every leaf. A phone can. Early detection of
leaf diseases means fewer pesticides, less crop loss. The user only needs to point a camera.

### The three teammates (memorize these roles)

| Part | Nickname | Language it speaks | Its one job |
|---|---|---|---|
| The **Android app** | The messenger | Kotlin | Collect the photo, send it, show the answer |
| The **Node.js server** | The receptionist | JavaScript | Receive the photo, hand it to the brain, return the answer |
| The **Python + TensorFlow worker** | The brain | Python | Look at the photo and produce the three answers |

Key sentence to internalize: **"Three separate programs, in two languages, on two machines (phone and
server), talking by sending an image out and a small JSON text back."**
If you can say that sentence and defend every word in it, you can pass the project round.

### The clinic analogy (for learning only — see Part 4 for how to speak in the interview)

- The photo = a **sample** taken from the field.
- The app = the **receptionist** who packages the sample in a proper envelope (multipart) and couriers it
  to the lab, then hands the report back to you.
- The Node server = the **lab's front desk**: checks the envelope arrived intact, gives the sample a
  barcode (temp file), calls a technician (spawns Python), and when the report comes back, mails it to you.
- The Python worker = the **technician**: cuts the sample into 24 prepared slides (crops and rotations),
  runs them past the specialist.
- The model = the **specialist's trained eye** — trained on thousands of past samples so it recognizes
  patterns, not because it "thinks."
- The three answers = three specialists each giving one verdict: the **botanist** (which plant), the
  **category expert** (type of illness), the **disease expert** (specific disease).
- Majority vote = the lab looks at all 24 slides and each specialist goes with what they saw on
  **most** slides.
- The label files = the **handbook** that converts the specialist's internal code numbers into
  human words ("Lettuce", "Fungal", "Downy Mildew").
- The JSON line on stdout = the **lab report** written in a strict one-line format.
- The popup = showing the report to the patient (the user).

> In the interview, do NOT use the analogy words. Use the real names — bolded in Part 2 — because each
> story step has a technical name you are expected to know. The analogy is the scaffolding in your head.

---

## Part 2 — The journey of one photo (learn this as a story)

Every technical term is **bold** the first time and instantly explained. If you can retell this story,
you can explain the project.

### The story, told slowly

**Step 1 — You choose a leaf photo.**
You open the app's gallery screen. Where do the photos come from? Android keeps a system-wide catalog of
all photos on the device, called **MediaStore**. The app asks MediaStore for every image's ID and the date
it was taken, sorts them newest-first, and groups them into sections: "RECENT" (this week), "LAST WEEK",
then by month, then by year. The grouping math lives in a **ViewModel** — a container that holds data so
it survives the phone being rotated (otherwise the list would reload every time you tilt the phone).
Each photo is identified by a **content URI** — a phone-internal address like
`content://media/external/images/media/123` that means "image number 123 in the shared photo catalog".

**Step 2 — You tap Submit.**
The app now needs to send the actual photo bytes. If the photo came from the gallery, its URI is an
address, not a file — so the app first **copies the bytes** into its own private folder on the phone
(apps are allowed to read/write their own folders without asking permission).

**Step 3 — The photo is put in an envelope.**
You cannot just throw raw photo bytes onto the internet. The app wraps them in an **HTTP request** —
think of it as an envelope with a delivery address and a content note. Specifically:
**HTTP POST** (an envelope that carries a body, unlike GET which only asks for something), to the
address `/image` on the server. The body format is **multipart/form-data** — an envelope that can hold
binary files, with the photo labeled with the field name **"image"**.
The library that builds and sends this envelope is **Retrofit** (a popular Android networking library)
running on top of **OkHttp** (the actual low-level HTTP engine underneath it).
Because the server is slow (step 7 explains why), the app explicitly sets OkHttp's **timeouts**
(patience limits) to 60 seconds for reading the reply — the default patience is only 10 seconds, which
is shorter than the server takes (more in Part 5, concept 7).

**Step 4 — The sending does not freeze the phone.**
Network calls take seconds; a phone screen must update 60 times per second or it looks frozen. Android
forbids doing slow work on the **main thread** (the UI thread that draws the screen). So the app uses
**Kotlin coroutines** — Kotlin's way to say "start this long task elsewhere, and resume here with the
result, without blocking." The Retrofit call is a **suspend function** (a function that can pause without
freezing the thread), launched in a **coroutine scope**; when the answer arrives, the code hops back to
`Dispatchers.Main` (the main thread) to update the screen safely.

**Step 5 — The server receives the envelope.**
The server program is written in **Node.js** (JavaScript) using **Express** — a minimal framework that
routes requests to your code: "a POST to /image goes to the image handler." The first thing Express does
for this request is run **Multer** — a middleware (a helper that runs on every matching request) that
specializes in parsing multipart envelopes. Multer streams the photo to a **temporary file** on the
server disk with a random hex name, and fills a variable `req.file` describing it. If the envelope had no
photo, the handler immediately replies **400** (the HTTP code meaning "your request was invalid") —
this check used to be missing and the server crashed instead.

**Step 6 — The server calls the brain.**
The server starts a separate **process** (an independently running program) — it **spawns Python**,
passing arguments like a work order: the temp file's path, plus the three labels to fill in
("plant_name", "disease_category", "disease_name"). Using **spawn** (which takes a list of arguments,
not one command string) means no shell is involved — there is no way for anyone to smuggle extra
commands into that call.

**Step 7 — The brain prepares the photo (preprocessing).**
Raw photos come in all sizes and shapes, but the model only accepts a fixed input: **224 x 224 x 3**
(224 pixels wide, 224 tall, 3 color channels red/green/blue). So the Python worker:
- Converts the image to plain RGB (phones sometimes store grayscale or extra transparency channels).
- Rotates it 4 ways: 0, 90, 180, 270 degrees — because a leaf photo can be taken at any orientation.
- Resizes so the **shorter side is 256 pixels**, keeping the aspect ratio (no squashing).
- From each rotation, cuts **five 224x224 crops**: the four corners plus the center — a disease spot
  might be anywhere in the frame, and each crop gives the model a close-up view of one region.
- Plus one extra copy of the center crop with a random tweak (**data augmentation**: flip, brightness,
  contrast, sharpness, or color shift) — making the model robust to lighting/camera differences.
- Total: 4 rotations x 6 = **24 tensors** (a *tensor* is just a grid of numbers — to the model, an image
  is a 224x224x3 block of pixel values).

**Step 8 — The model looks at all 24.**
The **model** is a **neural network** — roughly, millions of small adjustable number-multipliers
("parameters") arranged in layers, where earlier layers learn simple patterns (edges, blotches, colors)
and later layers learn concepts (leaf texture, spot shapes). This specific one is **MobileNet V1**
(4.28 million parameters), a small network designed to run on phones — reused here on the server.
It was **pretrained on ImageNet** (a famous 1.4-million-photo dataset of 1000 everyday categories):
it arrived already knowing how to "see," which is **transfer learning** — start from a network that
already knows general vision, attach small new parts for your specific task, train those. Here the new
parts are three tiny output layers ("**heads**"): one with 10 outputs (plants), one with 4 (categories),
one with 14 (diseases). One look at a photo = all three answers at once.
`model.predict` returns, for each of the 24 crops, a probability list per head (e.g., "crop #7:
92% looks like Lettuce").

**Step 9 — Voting and naming.**
For each head and each crop, take the **argmax** (the index of the highest probability — "pick the
winner"). Now each head has 24 opinions. The worker takes the **majority vote** across the 24 — e.g.,
"Lettuce won 21 of 24 crops." Finally it translates winning index numbers into words using two static
JSON files (`num_to_label_file.json`): index 2 -> "Lettuce", 1 -> "Healthy", and so on. For healthy
plants the disease name is an **empty string** by design (a healthy leaf has no disease).

**Step 10 — The report goes back, twice.**
Python prints exactly **one line of JSON** (a standard text format for structured data:
`{"plant_name":"Lettuce","disease_category":"Healthy","disease_name":""}`) to **stdout** (a program's
standard output channel), logs anything else to **stderr** (a separate channel for logs that must not
mix with data), and deletes the temp file. Node collects the stdout pieces (data can arrive in chunks,
so you glue chunks and parse once at the end) and replies **200** (HTTP "OK") with the JSON.

**Step 11 — The app shows the answer.**
Retrofit hands the JSON to **Gson** (a converter between JSON and Kotlin objects), which fills a small
data class called **PlantData**. Back on the main thread, the app hides the progress bar and shows a
**PopupWindow** (a floating box) — a green "Healthy" box, or a box listing plant + category + disease.
If anything went wrong anywhere (server down, malformed reply, timeout), the app catches the exception
and shows a toast: "Some Error Occurred."

### The same story in 8 technical steps (recite THIS version)

1. App picks a photo (gallery via **MediaStore** URIs, grouped by date in a **ViewModel**).
2. Photo bytes copied to the app's private folder if needed.
3. **Retrofit/OkHttp** wraps it in a **multipart POST** (field `"image"`) — with 30/60/30s timeouts —
   from a **coroutine**, UI thread untouched.
4. **Express + Multer** save the upload to a temp file; missing file = **400**.
5. Node **spawns Python** with (path, `plant_name`, `disease_category`, `disease_name`).
6. Python: RGB -> 4 rotations -> short-side-256 resize -> 5 crops + 1 augmented = **24 tensors**.
7. **MobileNet V1** (ImageNet-pretrained, 3 heads: 10/4/14) predicts all 24; per head:
   **argmax** -> **majority vote** -> label map -> one JSON line on **stdout**; deletes temp file.
8. Node parses stdout, replies **200 + JSON**; Gson fills **PlantData**; popup shows the result.

### If you remember NOTHING else, remember these five sentences

1. "Photo goes in, three labels come out."
2. "The app is Kotlin, the server is Node/Express, the brain is Python/TensorFlow."
3. "The model is a pretrained MobileNet with three output heads."
4. "One photo becomes 24 crops and rotations, and each head takes a majority vote."
5. "The whole round trip takes about 15-20 seconds, mostly because the model file is re-loaded on
   every request."

---

## Part 3 — Dictionary of every term you might say

Format: plain definition -> what it means in THIS project -> one deeper follow-up you may get.

### A. Phone-side terms

**Android** — the operating system on ~70% of phones. Here: the platform the user-facing app runs on.
*Deeper:* minSdk 23 means it runs on Android 6.0 and newer.

**Kotlin** — the modern language Google recommends for Android (cleaner, null-safer than Java; compiles to
the same bytecode). *Deeper:* our coroutines feature is Kotlin-specific; Java would need threads/callbacks.

**Activity** — one screen of an app, a class Android manages (created, shown, paused, destroyed as you
navigate). Here: four activities — Main (demo), Camera, Gallery, Submit. *Deeper:* no fragments; each
screen registers in AndroidManifest.xml.

**AndroidManifest.xml** — the app's contract with the OS: declares every screen, every permission, and
the launcher entry point. *Deeper:* we added `android:exported="true"` to the launcher — mandatory from
Android 12 for anything other apps can start.

**Permission (runtime)** — apps must ask the user live for sensitive things (camera, storage), not just
declare them. *Deeper:* our gallery query needs READ_EXTERNAL_STORAGE granted at runtime; if the user
hasn't granted it yet, the query returns nothing — which is why the empty-list crash guard matters.

**CameraX** — a modern camera library that wraps the messy old camera APIs. You describe use cases
(Preview, ImageCapture) and bind them to the activity's lifecycle. *Deeper:* `bindToLifecycle()` means
the camera auto-stops when the screen closes — no leaks.

**MediaStore** — Android's built-in catalog of all device photos/videos, queryable like a database.
*Deeper:* we query `_ID` + `DATE_TAKEN`, newest first, and build content URIs from the IDs.

**Content URI** — a phone-internal address (`content://...`) for data owned by another provider.
*Deeper:* not a file path; that's why the app copies gallery images to its own folder before upload.

**RecyclerView** — the workhorse list component: it reuses a handful of item views while you scroll
(so 10,000 photos cost 10 views, not 10,000). *Deeper:* an **Adapter** feeds it items and creates
ViewHolders (a small item wrapper it recycles); our gallery has a list-of-sections where each section is
itself a grid RecyclerView.

**Adapter** — the bridge between data and list UI: "given position N, show this."
*Deeper:* ours chooses the view type by `position % 2` — even = section header, odd = photo grid.

**ViewModel** — a data container that outlives screen rotation. *Deeper:* our gallery grouping
(RECENT/LAST WEEK/months/years) lives in ViewModels so rotating doesn't rescan MediaStore.

**Coroutines** — Kotlin's lightweight "start this later, resume me with the result" mechanism; the
thread is never blocked, it just moves on. *Deeper:* `suspend fun` = a function that can pause;
`launch(Dispatchers.Main)` = resume on the UI thread; `SupervisorJob` = one failed child doesn't cancel
its siblings.

**Main thread (UI thread)** — the single thread allowed to touch the screen; slow work on it = frozen UI,
and Android blocks network on it outright (NetworkOnMainThreadException). *Deeper:* that's the entire
reason for coroutines here; also why MainActivity's `runBlocking` (which DOES block the main thread) is
labeled known-debt.

**Retrofit** — a declarative HTTP client: you write an interface with annotations, it builds the calls.
*Deeper:* our interface has ONE method: `@Multipart @POST("image") suspend fun sendImage(@Part ...)`.

**OkHttp** — the actual HTTP engine under Retrofit; owns connections and timeouts.
*Deeper:* its default read timeout (10s) was shorter than our server's response time (~15-20s) — the app
never worked until we set 30/60/30s explicitly.

**Gson** — JSON-to-object converter. *Deeper:* `@SerializedName("plant_name")` maps the server's key to
our Kotlin property name; a mismatch = silently null fields.

**Glide** — an image-loading library: async decode + memory/disk caching + smooth in scrolling lists.
*Deeper:* loading content URIs in the gallery grid without it = jank and crashes.

**PopupWindow** — a floating view on top of the screen. *Deeper:* ours shows the Healthy card or the
Disease card and dismisses on touch.

**Gradle / AGP** — Gradle compiles/builds the project; the Android Gradle Plugin (AGP) teaches it how to
build Android apps. *Deeper:* versions matter — AGP 4.2 needs Gradle 6.7.1 and JDK 11; build-tools must
be 30.0.1 because 30.0.3 was never released for Windows.

**APK** — the packaged, installable Android app file. *Deeper:* our debug APK is ~5.6 MB — small because
the 51 MB model is NOT in the app; it lives on the server.

### B. Network terms

**Client / Server** — client initiates requests (the phone), server waits and answers (the computer
running Node + Python). *Deeper:* here there are technically three programs but only two machines:
phone (client) and server machine (Node and Python both live there).

**HTTP** — the request/response language of the web: you send a method + address + headers + optional
body; you get a status code + body. *Deeper:* our flow uses POST (send data) only; GET / just serves a
static page.

**POST** — the HTTP method meaning "here is data, process it" (vs GET = "give me a page").
*Deeper:* sent to path `/image` — the third segment of the flow's address.

**multipart/form-data** — an HTTP body format for shipping binary files: parts separated by a boundary
line, each part with its own headers (name, filename, content type). *Deeper:* exactly what
Retrofit's @Multipart produces and Multer consumes; the file's field name must be `"image"` on both ends.

**JSON** — JavaScript Object Notation, the standard text format for structured data:
`{"plant_name":"Lettuce"}`. *Deeper:* our contract has exactly three keys, matching constants.js,
the Python argv, and the app's @SerializedName annotations.

**Status codes** — the reply's verdict: 200 = OK, 400 = your request was invalid (client's fault),
500 = server-side failure. *Deeper:* missing file -> 400; python missing/crashed/bad output -> 500.

**Latency** — time from sending to answer arriving. *Deeper:* ours is ~15-20 s: ~6.6 s model load +
~1.2 s inference + process/IO overhead, because the model is loaded fresh per request.

**Timeout** — a patience limit; if no answer within it, the call fails. *Deeper:* THE bug of this
project: default 10 s < needed 20 s. Fixed with explicit 30/60/30 s (connect/read/write).

**Port (3000)** — one machine runs many services; a port is the apartment number where a service listens.
*Deeper:* the app's base URL includes `:3000`; behind a real domain you would hide it behind nginx TLS.

**LAN IP / cleartext** — the server is addressed by a local network IP (`192.168.1.8`) over plain HTTP.
*Deeper:* why the manifest needs `usesCleartextTraffic="true"`, and why this is unacceptable in production
(no privacy, no tamper protection) — the fix is HTTPS.

### C. Server-side terms

**Node.js** — JavaScript running outside the browser (on a server). *Deeper:* chosen for a tiny web tier:
Express + Multer + spawning Python is ~100 lines.

**Express** — the minimal Node web framework: middleware chain + routers that map (method, path) to
handlers. *Deeper:* our chain is morgan (logs) -> body parsers -> cookieParser -> static files -> routers.

**Middleware** — a helper that runs on the way to your handler and can act first (log, parse, guard).
*Deeper:* `upload.single('image')` is route-level middleware — it parses the multipart envelope and
fills req.file BEFORE our code runs.

**Route / handler** — the code for one address, e.g. `POST /image`. *Deeper:* ours guards req.file (400),
spawns Python, accumulates stdout, responds 200/500 once (a `responded` flag prevents double replies).

**Multer** — the multipart specialist for Express: streams uploads to disk with random names.
*Deeper:* disk storage (not memoryStorage) so big files don't fill RAM; it does NOT create its folder —
the repo ships `image/.gitkeep` so fresh clones work.

**Temp file** — a short-lived scratch file; here the uploaded photo under `Backend/image/`.
*Deeper:* Python deletes it after predicting; files leaked by crashes need a cleanup job (listed as
improvement).

**Process / spawn** — a process is a running program; spawn starts one as a child with an argument list.
*Deeper:* `spawn('python', [script, path, ...])` — no shell, so no command injection; contrast exec(),
which uses a shell string and buffers output.

**stdout / stderr** — a program's two standard output channels: stdout for DATA, stderr for LOGS.
*Deeper:* our contract is "exactly one JSON line on stdout" — which is why the Keras progress bar
polluting stdout was a real bug (fixed with `verbose=0`).

**Exit code** — a number a process leaves behind: 0 = success, anything else = failure.
*Deeper:* we deliberately respond based on stdout parse success, not exit code alone.

**Debug / Morgan** — logging libraries. *Deeper:* morgan logs every request line; npm audit drove
upgrading them (morgan 1.12.x fixes log forging).

### D. Machine learning terms (read slowly — this is where most students are weakest)

**AI / ML / deep learning (nested dolls)** — AI is the broad field ("machines doing smart-seeming
things"); machine learning is learning patterns FROM DATA instead of hand-written rules; deep learning is
ML using multi-layer neural networks. This project: deep learning, image classification.
*Deeper:* nobody hand-coded "what powdery mildew looks like" — the network learned it from examples.

**Neural network** — stacked layers of tiny number-multipliers ("neurons"). Training = showing labeled
examples and nudging millions of numbers until the network's guesses match the labels. After training,
the numbers ("weights") are frozen. *Deeper:* ours has 4.28 million of them ("parameters").

**CNN (convolutional neural network)** — the network type for images: early layers slide small pattern
filters across the picture (detecting edges, spots, textures), deeper layers combine those into
concepts. *Deeper:* "conv_dw"/"conv_pw" layers in our summary are MobileNet's cheap factorized filters.

**MobileNet V1** — Google's small, fast CNN family designed for phones. *Deeper:* full-size networks
(ResNet-50, 96MB, 25M params) would work too but cost 5-10x the compute; ours is 4.28M params, ~51 MB.

**Pretrained / ImageNet / transfer learning** — ImageNet is a famous 1.4M-photo, 1000-category dataset
(cats, cars, cups...). "Pretrained" = the network already learned general vision on it. Transfer learning
= reuse that trained "eye" for your task, attach small new layers, train only those on YOUR data.
*Deeper:* we kept even MobileNet's 1000-output layer and attached our three heads to it — unusual but
it works (verified end to end).

**Head (output layer)** — the final layer producing one task's answers. Ours has THREE heads on one
backbone: 10 (plant), 4 (category), 14 (disease). *Deeper:* one forward pass gives all three answers —
that's multi-task learning; the quirk is the category head is named `disease_catetgory` (typo) inside
the model file.

**Classification vs detection** — classification answers "what is this photo of?" (ours);
detection answers "what is it AND where in the frame?" (bounding boxes). *Deeper:* our crops approximate
"where" by brute force — we look at every region.

**Class / label** — one answer option: "Lettuce" is a class of the plant head. Labels are stored as
numbers internally (Lettuce = 2), converted via JSON label files. *Deeper:* 10 + 4 + 14 classes;
disease index 2 is an empty string — "healthy" plants have no disease name.

**Softmax** — converts a head's raw scores into probabilities that sum to 100% (0.9 Lettuce, 0.05
Mint, ...). *Deeper:* danger: it ALWAYS picks winners even for a cat photo — no built-in "none of these".

**argmax** — "give me the INDEX of the highest value" — i.e., which class won. *Deeper:* we run argmax
per crop per head (24 votes), not once.

**Preprocessing** — making raw input model-ready (resize, crop, color normalize). *Deeper:* our exact
pipeline: RGB convert -> 4 rotations -> short-side-256 -> five 224 crops + one augmented crop = 24.

**Data augmentation** — deliberately varying training images (flips, brightness, rotation) so the model
doesn't memorize one camera angle. *Deeper:* we also use augmentation AT PREDICTION time (next term).

**TTA (test-time augmentation) / majority voting** — at prediction: instead of one photo, send 24
variants; each of 24 predictions votes; the most common answer wins ("Lettuce: 21/24 votes").
*Deeper:* this is why one photo = 24 model inputs, and why vote share 21/24 is a natural confidence
score we should report (currently discarded — known improvement).

**Inference (vs training)** — training = learning from data (not in this repo); inference = using the
frozen model on new inputs (what this repo does). *Deeper:* our inference costs ~1.2 s; the model LOAD
(~6.6 s) is the expensive part, repeated per request.

**SavedModel** — TensorFlow's save format: a folder with the graph (saved_model.pb) + weights
(variables/). *Deeper:* language-agnostic and servable; ~51 MB here; .h5 is the legacy single-file
format; .tflite is the quantized phone format.

**Parameters** — the learned numbers (weights) inside the network. *Deeper:* ours: 4,281,892 — a good
size to quote; "the model file" is mostly these numbers serialized to disk.

**Overfitting** — memorizing training photos instead of learning general patterns; shows up as great
training accuracy, bad real-world accuracy. *Deeper:* mitigations visible in this project: pretrained
backbone, augmentation, small heads, majority voting. Without stored eval metrics we can't measure it —
say exactly that if asked (see INTERVIEW_PREP.md 17.1).

**Quantization / TFLite** — shrinking a model to 8-bit numbers (int8) so it runs on a phone:
51 MB -> roughly 5-15 MB, sub-second. *Deeper:* the strongest v2 alternative to server inference
(offline, private) — know the trade-off (updates then require an app release).

**OOD (out-of-distribution) input** — an input unlike anything in training (a cat, a screenshot).
*Deeper:* softmax still answers confidently — the fix is confidence thresholds or a "not a leaf"
rejector; volunteering this shows real understanding.

---

## Part 4 — Interview scripts (memorize, then say aloud 5 times)

Rule: **never say a word you cannot define.** Every bolded word below is defined in Part 3.

### Script A — 30 seconds, non-technical (HR / manager)

> "It's a plant doctor in your pocket. You photograph a leaf; the app sends the picture to our server;
> a machine learning model — trained on thousands of leaf photos — identifies the plant, tells you if
> it's healthy, and if it's sick, which disease it has. The answer appears on screen in about twenty
> seconds. The advantage: farmers can spot disease early without waiting for an expert."

### Script B — 2 minutes, technical (your main answer)

> "There are three parts. The Android app is written in Kotlin: a camera screen using CameraX, a gallery
> that groups the device's photos by date using MediaStore, and a submit screen. When the user submits,
> the app uploads the image with Retrofit as a multipart POST request, from a coroutine so the UI never
> blocks.
>
> The backend is Node.js with Express. A route on POST /image uses Multer to save the upload to a temp
> file, then spawns a Python process with the file path and the three label names as arguments.
>
> Python does the ML. It loads a TensorFlow Keras SavedModel — a MobileNet V1 backbone pretrained on
> ImageNet, with three output heads: ten plants, four disease categories, and fourteen diseases.
> It prepares twenty-four views of the photo — four rotations, and per rotation five 224-by-224 crops
> plus one augmented crop — runs them through the model, takes the argmax per head, majority-votes across
> the views, maps the winning indices to human labels, and prints the result as one JSON line on stdout.
>
> Node parses that, replies 200 with the JSON, and the app uses Gson to fill a small data class and show
> the result popup. End to end it takes fifteen to twenty seconds, mostly because the model is reloaded
> for each request — the first thing I'd fix with a persistent model server."

### Script C — "Explain it to a farmer"

> "You take a picture of the sick leaf and press send. My program looks at that picture the way a
> trained expert would — it has studied thousands of leaves before. Twenty seconds later it tells you:
> this is lettuce, and it has downy mildew, a fungal disease — instead of just guessing."

(Then immediately offer: "Technically, the picture goes to a server where a neural network classifies it."
— interviewers like plain-language PLUS the correct term.)

### Script D — The whiteboard version (what to draw, in order)

```
   PHONE                SERVER (one machine)
+-------------+        +-----------------------------------------+
| Kotlin app  |  1     |  Node + Express                          |
| Retrofit ---|--(multipart POST /image)--> Multer -> temp file  |
| coroutine   |        |   2. spawn python [path + 3 labels]      |
|             |        |         |                               |
| Gson <-Popup|  4     |   3. Python + TF: 24 crops -> MobileNet |
|             |<-------|------ one JSON line (stdout) ---^         |
|             | (200)  |   Node parses JSON, replies 200         |
+-------------+        +-----------------------------------------+
```
Draw the boxes first, then arrows 1-4, then annotate: "multipart, field 'image'",
"24 = 4 rotations x (5 crops + 1 augmented)", "3 heads: 10/4/14", "JSON keys match on both ends".

---

## Part 5 — The 7 ideas you must understand deeply (each in 3 layers)

Interviewers do not ask "what is X" — they ask "WHY is X like that HERE". Each idea: the simple truth,
the precise version, and why this project chose it.

### Idea 1 — Why is the model on a server, not inside the phone?

- Simple: the brain is 51 MB and updates often — keep it in one place you control, not in every pocket.
- Precise: server-side inference decouples model updates from app releases, keeps the APK at 5.6 MB,
  and gives identical quality on cheap phones. Cost: needs internet, ~15-20 s round trip.
- Why here: this architecture = "retrain the model tonight, every user has it tomorrow." The on-device
  alternative (TFLite, int8, ~5-15 MB, sub-second, offline, private) is the honest v2 debate — mention
  both, say which you'd pick for rural users (on-device) and why (connectivity).

### Idea 2 — What does "pretrained" actually buy us?

- Simple: it already knows how to see, so we only teach it leaves.
- Precise: ImageNet pretraining gives the backbone general visual features (edges, textures, shapes).
  Our dataset of 10 herbs is far too small to train a network from scratch; attaching three tiny Dense
  heads (10k-14k params each) to frozen pretrained features works with hundreds-not-millions of images.
- Why here: transfer learning is THE reason small datasets are viable — say this sentence exactly:
  "You almost never train a vision network from scratch in 2024; you fine-tune."

### Idea 3 — Why 24 crops and a majority vote?

- Simple: the disease spot can be anywhere in the photo, and you don't know which way is "up" for a leaf.
- Precise: rotations (0/90/180/270) cover orientation; five 224-crops cover location; one augmented crop
  covers lighting. Each of the 24 gets a vote per head; the mode wins. It's TTA — trading 24x compute for
  robustness (compute that a server can afford).
- Why here: also gives a free confidence measure — 21/24 is much stronger than 13/24 — that the current
  code throws away. "I'd report votes/24 as confidence" is a gold-star sentence.

### Idea 4 — Why three heads instead of one big classifier?

- Simple: the answer naturally has three parts (who, sick-or-not, what sickness).
- Precise: one 14-way head loses plant/category context; a 10x4x14 joint head is hopelessly sparse.
  Three heads share one backbone = one forward pass, regularization from multi-task learning, and
  partial credit when one head is wrong.
- Why here + the flaw to volunteer: the heads are independent, so "Healthy" can co-occur with
  "Mosaic Virus" — no consistency constraint exists; post-processing masks would fix it.

### Idea 5 — What is "uploading" really?

- Simple: putting the photo inside a labeled envelope addressed to the server.
- Precise: an HTTP POST whose body is multipart/form-data: boundary, part headers (name="image",
  filename, content type), raw JPEG bytes. Retrofit's @Part builds it; Multer parses it; the field name
  "image" is a hard contract on both ends.
- Why here: alternative (base64-in-JSON) inflates the payload ~33% — always mention that number.

### Idea 6 — What happens when things fail?

- Simple: every stage has a fallback; nothing should crash silently.
- Precise: no file -> 400; python missing/crashed/bad output -> 500 generic JSON (no stack leaks);
  network/timeout on the phone -> caught exception -> toast; empty gallery -> guarded sort function;
  grayscale/odd PNG -> converted to RGB.
- Why here: "give me all your failure modes" is a favorite question — recite this list from memory.

### Idea 7 — Why did the app never work, even when the server was healthy? (THE story)

- Simple: the phone gave up listening before the answer arrived.
- Precise: OkHttp's default read timeout is 10 seconds; our prediction takes 15-20 (model reload).
  So every request died with SocketTimeoutException while a curl test succeeded — the client's
  defaults, not the server, were broken. Fix: explicit 30/60/30 s timeouts.
- The lesson (say it): "A green server and a green app can still make a red product. You must test the
  REAL client against the REAL server — 'works on curl' is not 'works'."

---

## Part 6 — Phrases that will hurt you (never say these)

| Don't say | Why it hurts | Say instead |
|---|---|---|
| "The model is in the app" | It's on the server; the APK is 5.6 MB | "The model runs server-side; the app just uploads and displays" |
| "Firebase hosts my backend" | firebase.json only serves the static page | "Firebase hosts the one-page site; the Express server runs separately" |
| "The AI thinks / understands" | It computes pattern probabilities | "It classifies pixel patterns it learned from labeled examples" |
| "It's 95% accurate" (any invented number) | No eval exists in the repo — inventing is fatal | "The repo ships no stored eval; I'd measure per-head accuracy and macro-F1 on a stratified holdout" |
| "We have a database" | There is none — static JSON label files | "Label mappings are static JSON files; the server is stateless" |
| "Kotlin is the backend" | Backend is Node.js; Python is the ML | "Kotlin app, Node server, Python ML worker" |
| "The app sends the photo to Python directly" | Node sits in between | "App -> Node -> spawned Python -> back the same path" |
| "24 photos" | It's 24 processed views of ONE photo | "24 tensors derived from one photo: 4 rotations x (5 crops + 1 augmented)" |
| "I used ML libraries so I didn't need to know the math" | Follow-ups will find you | Be ready for argmax/softmax/majority vote — all defined in Part 3D |

---

## Part 7 — Flashcards (cover the right column; say the answer aloud)

| Term | One-line answer |
|---|---|
| 3 parts of the system | Kotlin app (messenger), Node/Express server (receptionist), Python/TensorFlow worker (brain) |
| 3 model heads | plant (10), disease category (4), disease (14) |
| The 4 categories | Fungal, Healthy, Viral, Bacterial |
| Backbone | MobileNet V1, ImageNet-pretrained, 4.28M params, ~51 MB |
| Model input size | 224 x 224 x 3 (RGB) |
| 24 = ? | 4 rotations x (5 crops: 4 corners + center, plus 1 augmented center) |
| Resize rule | shorter side to 256, aspect ratio kept, then crop 224 |
| Aggregation | argmax per head per crop, then majority vote across 24 |
| Label translation | num_to_label_file.json: index -> name; healthy = empty disease name |
| Upload format | HTTP POST multipart/form-data, field name "image" |
| Networking stack (app) | Retrofit interface -> OkHttp engine -> Gson converter |
| App timeouts | 30 s connect / 60 s read / 30 s write (defaults 10 s caused the famous bug) |
| Server upload handler | Express route + Multer .single('image') -> temp file |
| Node calls Python via | spawn(process) with argv [script, path, 3 labels] |
| Python->Node reply | exactly one JSON line on stdout; logs to stderr; deletes temp file |
| The typo in the model | Dense layer named 'disease_catetgory' in metadata; code resolves heads robustly |
| Latency + its parts | ~15-20 s = ~6.6 s model load + ~1.2 s inference + overhead |
| 400 vs 500 | 400 = client error (no file); 500 = server error (python/spawn/parse failed) |
| Android versions | minSdk 23 (Android 6.0), target/compile 30 |
| Camera library | CameraX bound to lifecycle; image saved via ImageCapture use case |
| Gallery source | MediaStore query (ID + date), grouped RECENT/LAST WEEK/months/years in ViewModels |
| Coroutines role | suspend upload off the main thread; Dispatchers.Main for the popup |
| Pretrained meaning | ImageNet-learned vision features reused = transfer learning |
| Softmax | turns scores into probabilities summing to 1 — always picks winners, even for a cat |
| TTA | test-time augmentation: predict on transformed copies, aggregate votes |
| SavedModel | TF save format: saved_model.pb + variables/ folder |
| Why not in the phone? | updates without app releases, tiny APK; trade-off: needs internet |
| Firebase's real role | static hosting only (public/ page), not the API |
| The one-liner | "Photo in, three labels out: 24 views, one vote, one JSON line." |

---

## Part 8 — Your study plan (3 days, ~2 hours/day)

**Day 1 — Own the story.**
- Read Parts 1-2 twice, out loud the second time.
- Close the file. Tell the journey of the photo from memory, in the 8-step version.
- Reopen, check yourself, repeat until the 8 steps come without peeking.
- Then say the five "remember nothing else" sentences.

**Day 2 — Own the words.**
- Part 3 in two sittings: A+B in the morning, C+D in the evening.
- For each term: read, cover, explain aloud in one sentence, including "in this project".
- Evening: Part 7 flashcards until you can do all of them twice with no misses.

**Day 3 — Own the interview.**
- Morning: memorize Script B (2-minute technical). Deliver it to a wall/mirror/friend 5 times.
- Then Part 5: for each of the 7 ideas, say the simple layer, then the precise layer, from memory.
- Afternoon: read Part 6 once; then quiz yourself on the mock interview (Part 19 of INTERVIEW_PREP.md).
- Final hour: graduate to INTERVIEW_PREP.md — sections 5 (numbers), 10 (pick your 2 war stories:
  the timeout and stdout), and 17 (read twice).

**The bar to clear before you stop:** a friend asks "so what is this project?" and you talk for
2 minutes using Script B without notes — and then they pick ANY bolded word you said and ask "what's
that?" and you can answer in one plain sentence. When both happen, you are interview-ready.

---

*Companion to INTERVIEW_PREP.md (the full reference with 65+ Q&As, war stories, and system design).
Everything here describes the verified working state of this repository.*
