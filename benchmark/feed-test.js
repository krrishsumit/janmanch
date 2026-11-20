// k6 load test for the home feed. Install k6 from https://k6.io, then run:
//   k6 run benchmark/feed-test.js
// Run it twice (see benchmark/README.md) and compare "http_req_duration".

import http from "k6/http";
import { check } from "k6";

export const options = {
    vus: 100,            // 100 users at the same time
    duration: "60s",
    thresholds: {
        http_req_failed: ["rate<0.01"],
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
    // Most visitors look at the first few pages.
    const page = Math.floor(Math.random() * 5);
    const response = http.get(`${BASE_URL}/videos/feed?page=${page}&size=20`);
    check(response, { "status is 200": r => r.status === 200 });
}
