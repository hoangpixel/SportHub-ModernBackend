import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: Number(__ENV.VUS || 1),
    duration: __ENV.DURATION || '60s',
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)'],
    thresholds: {
        http_req_failed: ['rate==0'],
        checks: ['rate==1'],
    },
};

export default function () {
    const response = http.get(
        'http://localhost:8080/api/venues?page=0&size=10&sortBy=id&direction=asc',
        {
            timeout: '10s',
            tags: { name: 'GET venues paginated' },
        }
    );

    let body = null;

    try {
        body = response.json();
    } catch {
        body = null;
    }

    check(response, {
        'HTTP status is 200': result => result.status === 200,
        'response contains content array': () =>
            body !== null && Array.isArray(body.content),
    });

    sleep(1);
}