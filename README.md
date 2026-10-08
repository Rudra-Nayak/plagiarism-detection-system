# Text Similarity and Plagiarism Checker

<!-- CONFIRM: project name. Rename the title if you use a different one. -->

A plagiarism detection backend written in plain Java with no external dependencies. It compares a source text with a suspicious text and returns a similarity score, the exact overlapping passages, and performance metrics for the algorithms that produced them.

It is built around two classic algorithms: **cosine similarity** for how alike the two texts are overall, and **Knuth-Morris-Pratt (KMP)** string matching for locating the copied passages.

## Features

- **Similarity score:** cosine similarity between the two texts after tokenizing and normalizing them.
- **Exact match detection:** KMP matching returns every overlapping passage, with start and end positions in both the source and the suspicious text, so a frontend can highlight them.
- **Algorithm metrics:** each response includes execution time (in milliseconds), the number of tokens mapped, and a collision count from the similarity calculation.
- **No dependencies:** uses only the HTTP server built into the JDK (`com.sun.net.httpserver`). No Maven, Gradle or external libraries.
- **Browser-friendly:** CORS headers are set, so a frontend served from a different origin can call the API.

## How it works

1. The request carries two texts: `sourceText` and `suspiciousText`.
2. `preprocessing.TextNormalizer` tokenizes both texts into words.
3. `algorithms.SimilarityCalculator` computes the cosine similarity between them and records collisions.
4. `algorithms.KMPMatcher` finds the matching spans between the two texts.
5. The server measures the total processing time with `System.nanoTime()` and returns everything as JSON.

## API

### `POST /api/analyze`

Server: `http://localhost:8080`

**Request body (JSON)**

```json
{
  "sourceText": "The original text goes here.",
  "suspiciousText": "The text you want to check goes here."
}
```

**Response (JSON)**

```json
{
  "similarityScore": 0.82,
  "executionTimeMs": 1.2345,
  "collisions": 0,
  "tokensMapped": 12,
  "matches": [
    {
      "text": "the original text",
      "suspiciousStart": 0,
      "suspiciousEnd": 17,
      "sourceStart": 4,
      "sourceEnd": 21
    }
  ]
}
```

The values above only show the format.

| Field | Meaning |
| --- | --- |
| `similarityScore` | Cosine similarity of the two texts, to 2 decimal places |
| `executionTimeMs` | Total time spent analyzing, in milliseconds |
| `collisions` | Collision count reported by the similarity calculation |
| `tokensMapped` | Total number of words in both texts |
| `matches` | List of overlapping passages found by KMP |
| `matches[].text` | The matching passage |
| `matches[].suspiciousStart` / `suspiciousEnd` | Position of the passage in the suspicious text |
| `matches[].sourceStart` / `sourceEnd` | Position of the passage in the source text |

**Other responses**

- `OPTIONS /api/analyze` returns `204` (CORS preflight).
- Any method other than `POST` and `OPTIONS` returns `405`.
- If processing fails, the server returns `500` with `{"error": "Internal Server Error: ..."}`.

### Try it

```
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"sourceText":"The quick brown fox jumps over the lazy dog.","suspiciousText":"A quick brown fox jumps over a lazy dog."}'
```

## Getting started

**Requirements:** JDK 10 or newer.

From the project root (the folder that contains `api/`, `algorithms/` and `preprocessing/`):

```
javac -d out -sourcepath . api/AppServer.java
java -cp out api.AppServer
```

<!-- CONFIRM: if your sources live under a src/ folder, run these commands from inside src/. -->

The server starts on port 8080:

```
Backend AppServer is running on port 8080...
Endpoint available: POST http://localhost:8080/api/analyze
```

## Project structure

```
api/            AppServer: HTTP server and the /api/analyze handler
algorithms/     KMPMatcher, SimilarityCalculator
preprocessing/  TextNormalizer (tokenizing and normalizing text)
```

<!-- CONFIRM: add your frontend folder and any other files here. -->

## Design notes and limitations

- **Hand-written JSON handling.** The request parser and response builder are written by hand to avoid dependencies. The parser expects a simple flat JSON object with string values, and it does not decode `\uXXXX` escape sequences. A JSON library such as Jackson or Gson would be the next step for production use.
- **One request at a time.** The server uses the default executor, so requests are handled sequentially on a single thread. Fine for a demo or a coursework project, but not for concurrent load. A fixed thread pool would be the fix.
- **Open CORS.** `Access-Control-Allow-Origin` is set to `*` for easy local development. Restrict it before any public deployment.
- **No input size limit or authentication.**
- **Exact matching only.** KMP finds exact word-for-word matches, so paraphrased text is detected only through the overall cosine similarity score, not through highlighted passages.

## Tech

Java, `com.sun.net.httpserver`, Knuth-Morris-Pratt, cosine similarity
