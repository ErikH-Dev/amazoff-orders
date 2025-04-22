import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  cloud: {
    projectID: 3759181,
    name: 'Monolith App Load Test',
  },
  stages: [
    { duration: '30s', target: 1000 }, // Ramp-up to 1000 users over 30 seconds
    { duration: '1m', target: 1000 }, // Stay at 1000 users for 1 minute
    { duration: '30s', target: 0 }, // Ramp-down to 0 users over 30 seconds
  ],
};

export default function () {
  const baseUrl = 'http://localhost:8080'; // Replace with your app's base URL

  // Test the /orders endpoint
  const ordersRes = http.get(`${baseUrl}/orders`);
  check(ordersRes, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  // Test the /products endpoint
  const productsRes = http.get(`${baseUrl}/products`);
  check(productsRes, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  // Test the /vendors endpoint
  const vendorsRes = http.get(`${baseUrl}/vendors`);
  check(vendorsRes, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  // Test the /buyers endpoint
  const buyersRes = http.get(`${baseUrl}/buyers`);
  check(buyersRes, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  // Test the /addresses endpoint
  const addressesRes = http.get(`${baseUrl}/addresses`);
  check(addressesRes, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(1); // Wait for 1 second between iterations
}