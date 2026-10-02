import { useMutation } from "@tanstack/react-query";
import axios from "axios";

export default function App() {
  const runScrape = useMutation({
    mutationFn: async () => (await axios.post("/api/scraper/run")).data,
  });

  return (
    <div style={{ fontFamily: "system-ui", padding: 24, maxWidth: 720, margin: "0 auto" }}>
      <h1>Job Application Agent</h1>
      <p>Scrapes jobs every 3 hours, scores them with Claude, and tracks your pipeline.</p>

      <section style={{ marginTop: 24 }}>
        <h2>Trigger a scrape</h2>
        <button onClick={() => runScrape.mutate()} disabled={runScrape.isPending}>
          {runScrape.isPending ? "Running..." : "Run now"}
        </button>
        {runScrape.data && (
          <pre style={{ background: "#f4f4f4", padding: 12, marginTop: 12 }}>
            {JSON.stringify(runScrape.data, null, 2)}
          </pre>
        )}
      </section>
    </div>
  );
}