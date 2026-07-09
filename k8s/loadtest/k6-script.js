import http from "k6/http";
import { sleep } from "k6";

// Gera carga crescente para demonstrar o scale-up/scale-down do HPA.
// Uso: k6 run -e BASE_URL=http://localhost:8080 k6-script.js
// (com `kubectl port-forward svc/oficina-api 8080:80 -n oficina` rodando em outro terminal)
export const options = {
  stages: [
    { duration: "1m", target: 50 },
    { duration: "3m", target: 50 },
    { duration: "1m", target: 0 },
  ],
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
  http.get(`${BASE_URL}/actuator/health`);
  sleep(0.2);
}
