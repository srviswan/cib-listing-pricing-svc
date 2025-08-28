#!/usr/bin/env python3
"""
Stress Testing Script for Publishing Service
Tests service limits under extreme load conditions
"""

import asyncio
import aiohttp
import time
import statistics
import json
from datetime import datetime
from typing import List, Dict, Any
import random

class PublishingServiceStressTester:
    def __init__(self, base_url: str = "http://localhost:8083"):
        self.base_url = base_url
        self.results = []
        
    async def make_request(self, session: aiohttp.ClientSession, endpoint: str, method: str = "GET", 
                          data: Dict = None, headers: Dict = None) -> Dict[str, Any]:
        """Make a single HTTP request and measure response time"""
        start_time = time.time()
        
        try:
            if method == "GET":
                async with session.get(f"{self.base_url}{endpoint}", headers=headers) as response:
                    response_text = await response.text()
                    status_code = response.status
            elif method == "POST":
                async with session.post(f"{self.base_url}{endpoint}", 
                                      json=data, headers=headers) as response:
                    response_text = await response.text()
                    status_code = response.status
            else:
                raise ValueError(f"Unsupported method: {method}")
                
            end_time = time.time()
            response_time = (end_time - start_time) * 1000  # Convert to milliseconds
            
            return {
                "endpoint": endpoint,
                "method": method,
                "status_code": status_code,
                "response_time_ms": response_time,
                "success": 200 <= status_code < 300,
                "timestamp": datetime.now().isoformat()
            }
            
        except Exception as e:
            end_time = time.time()
            response_time = (end_time - start_time) * 1000
            
            return {
                "endpoint": endpoint,
                "method": method,
                "status_code": 0,
                "response_time_ms": response_time,
                "success": False,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }
    
    def generate_basket_data(self, basket_id: str) -> Dict[str, Any]:
        """Generate realistic basket data for testing"""
        symbols = ["AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "AMD", "INTC"]
        
        constituents = []
        total_weight = 0.0
        
        for i in range(random.randint(5, 10)):
            symbol = random.choice(symbols)
            weight = round(random.uniform(0.05, 0.25), 3)
            total_weight += weight
            
            constituents.append({
                "symbol": symbol,
                "symbolName": f"{symbol} Corporation",
                "weight": weight,
                "shares": random.randint(100, 10000),
                "sector": random.choice(["Technology", "Healthcare", "Finance", "Consumer", "Energy"]),
                "country": "US",
                "currency": "USD"
            })
        
        # Normalize weights to sum to 1.0
        for constituent in constituents:
            constituent["weight"] = round(constituent["weight"] / total_weight, 3)
        
        return {
            "basketId": basket_id,
            "basketCode": f"STRESS_{basket_id}",
            "basketName": f"Stress Test Basket {basket_id}",
            "basketType": "EQUITY",
            "baseCurrency": "USD",
            "totalWeight": 1.0,
            "constituents": constituents
        }
    
    async def run_stress_test(self, endpoint: str, method: str = "GET", 
                             concurrent_users: int = 50, total_requests: int = 1000,
                             data_generator=None, test_name: str = None):
        """Run a stress test for a specific endpoint"""
        print(f"\n🔥 Starting Stress Test: {test_name or endpoint}")
        print(f"   Endpoint: {method} {endpoint}")
        print(f"   Concurrent Users: {concurrent_users}")
        print(f"   Total Requests: {total_requests}")
        
        start_time = time.time()
        
        # Use connection pooling and limits for stress testing
        connector = aiohttp.TCPConnector(limit=concurrent_users, limit_per_host=concurrent_users)
        timeout = aiohttp.ClientTimeout(total=30)  # 30 second timeout
        
        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            # Create tasks for concurrent requests
            tasks = []
            
            for i in range(total_requests):
                if data_generator:
                    data = data_generator(f"STRESS_{i:04d}")
                else:
                    data = None
                
                task = self.make_request(session, endpoint, method, data)
                tasks.append(task)
            
            # Execute all requests concurrently
            responses = await asyncio.gather(*tasks, return_exceptions=True)
            
            # Filter out exceptions
            valid_responses = [r for r in responses if not isinstance(r, Exception)]
            exceptions = [r for r in responses if isinstance(r, Exception)]
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # Analyze results
        successful_requests = [r for r in valid_responses if r["success"]]
        failed_requests = [r for r in valid_responses if not r["success"]]
        
        response_times = [r["response_time_ms"] for r in successful_requests]
        
        if response_times:
            avg_response_time = statistics.mean(response_times)
            median_response_time = statistics.median(response_times)
            min_response_time = min(response_times)
            max_response_time = max(response_times)
            p95_response_time = statistics.quantiles(response_times, n=20)[18] if len(response_times) >= 20 else max(response_times)
            p99_response_time = statistics.quantiles(response_times, n=100)[98] if len(response_times) >= 100 else max(response_times)
        else:
            avg_response_time = median_response_time = min_response_time = max_response_time = p95_response_time = p99_response_time = 0
        
        # Calculate throughput
        requests_per_second = total_requests / total_time if total_time > 0 else 0
        
        # Store results
        test_result = {
            "test_name": test_name or endpoint,
            "endpoint": endpoint,
            "method": method,
            "concurrent_users": concurrent_users,
            "total_requests": total_requests,
            "successful_requests": len(successful_requests),
            "failed_requests": len(failed_requests),
            "exceptions": len(exceptions),
            "success_rate": len(successful_requests) / total_requests * 100,
            "total_time_seconds": total_time,
            "requests_per_second": requests_per_second,
            "avg_response_time_ms": avg_response_time,
            "median_response_time_ms": median_response_time,
            "min_response_time_ms": min_response_time,
            "max_response_time_ms": max_response_time,
            "p95_response_time_ms": p95_response_time,
            "p99_response_time_ms": p99_response_time,
            "responses": valid_responses,
            "exception_details": [str(e) for e in exceptions]
        }
        
        self.results.append(test_result)
        
        # Print results
        print(f"   ✅ Success Rate: {test_result['success_rate']:.1f}%")
        print(f"   ❌ Failed Requests: {len(failed_requests)}")
        print(f"   💥 Exceptions: {len(exceptions)}")
        print(f"   📊 Throughput: {test_result['requests_per_second']:.1f} req/sec")
        print(f"   ⏱️  Avg Response Time: {test_result['avg_response_time_ms']:.1f}ms")
        print(f"   📈 P95 Response Time: {test_result['p95_response_time_ms']:.1f}ms")
        print(f"   📊 P99 Response Time: {test_result['p99_response_time_ms']:.1f}ms")
        print(f"   🕐 Total Time: {test_result['total_time_seconds']:.2f}s")
        
        return test_result
    
    async def run_comprehensive_stress_test(self):
        """Run comprehensive stress testing across all endpoints"""
        print("💥 Starting Comprehensive Stress Testing for Publishing Service")
        print("=" * 80)
        
        # Test 1: Health endpoint - High load
        await self.run_stress_test(
            endpoint="/actuator/health",
            method="GET",
            concurrent_users=100,
            total_requests=1000,
            test_name="Health Check - High Load"
        )
        
        # Test 2: Health endpoint - Extreme load
        await self.run_stress_test(
            endpoint="/actuator/health",
            method="GET",
            concurrent_users=200,
            total_requests=2000,
            test_name="Health Check - Extreme Load"
        )
        
        # Test 3: Vendor health endpoint - High load
        await self.run_stress_test(
            endpoint="/api/v1/publishing/vendors/health",
            method="GET",
            concurrent_users=100,
            total_requests=1000,
            test_name="Vendor Health - High Load"
        )
        
        # Test 4: Vendor health endpoint - Extreme load
        await self.run_stress_test(
            endpoint="/api/v1/publishing/vendors/health",
            method="GET",
            concurrent_users=200,
            total_requests=2000,
            test_name="Vendor Health - Extreme Load"
        )
        
        # Test 5: Basket listing - High load
        await self.run_stress_test(
            endpoint="/api/v1/publishing/basket/STRESS_001/list",
            method="POST",
            concurrent_users=50,
            total_requests=500,
            data_generator=self.generate_basket_data,
            test_name="Basket Listing - High Load"
        )
        
        # Test 6: Basket listing - Extreme load
        await self.run_stress_test(
            endpoint="/api/v1/publishing/basket/STRESS_002/list",
            method="POST",
            concurrent_users=100,
            total_requests=1000,
            data_generator=self.generate_basket_data,
            test_name="Basket Listing - Extreme Load"
        )
        
        # Test 7: Mixed endpoints - Extreme load
        await self.run_mixed_stress_test(
            concurrent_users=150,
            total_requests=1500,
            test_name="Mixed Endpoints - Extreme Load"
        )
        
        # Test 8: Sustained load test
        await self.run_sustained_load_test(
            concurrent_users=75,
            duration_seconds=60,
            test_name="Sustained Load - 1 Minute"
        )
        
        print("\n" + "=" * 80)
        print("🎯 Stress Testing Complete!")
        self.print_summary()
    
    async def run_mixed_stress_test(self, concurrent_users: int = 150, 
                                   total_requests: int = 1500, test_name: str = "Mixed Stress"):
        """Run a mixed stress test across multiple endpoints"""
        print(f"\n🔥 Starting Mixed Stress Test: {test_name}")
        print(f"   Concurrent Users: {concurrent_users}")
        print(f"   Total Requests: {total_requests}")
        
        endpoints = [
            ("/actuator/health", "GET", None),
            ("/api/v1/publishing/vendors/health", "GET", None),
            ("/api/v1/publishing/basket/STRESS_MIXED_001/list", "POST", self.generate_basket_data),
        ]
        
        start_time = time.time()
        
        connector = aiohttp.TCPConnector(limit=concurrent_users, limit_per_host=concurrent_users)
        timeout = aiohttp.ClientTimeout(total=60)
        
        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            tasks = []
            
            for i in range(total_requests):
                endpoint, method, data_gen = random.choice(endpoints)
                
                if data_gen:
                    data = data_gen(f"STRESS_MIXED_{i:04d}")
                else:
                    data = None
                
                task = self.make_request(session, endpoint, method, data)
                tasks.append(task)
            
            responses = await asyncio.gather(*tasks, return_exceptions=True)
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # Analyze mixed results
        valid_responses = [r for r in responses if not isinstance(r, Exception)]
        exceptions = [r for r in responses if isinstance(r, Exception)]
        
        successful_requests = [r for r in valid_responses if r["success"]]
        failed_requests = [r for r in valid_responses if not r["success"]]
        
        response_times = [r["response_time_ms"] for r in successful_requests]
        
        if response_times:
            avg_response_time = statistics.mean(response_times)
            p95_response_time = statistics.quantiles(response_times, n=20)[18] if len(response_times) >= 20 else max(response_times)
        else:
            avg_response_time = p95_response_time = 0
        
        requests_per_second = total_requests / total_time if total_time > 0 else 0
        
        print(f"   ✅ Success Rate: {len(successful_requests) / total_requests * 100:.1f}%")
        print(f"   ❌ Failed Requests: {len(failed_requests)}")
        print(f"   💥 Exceptions: {len(exceptions)}")
        print(f"   📊 Throughput: {requests_per_second:.1f} req/sec")
        print(f"   ⏱️  Avg Response Time: {avg_response_time:.1f}ms")
        print(f"   📈 P95 Response Time: {p95_response_time:.1f}ms")
        print(f"   🕐 Total Time: {total_time:.2f}s")
    
    async def run_sustained_load_test(self, concurrent_users: int = 75, 
                                     duration_seconds: int = 60, test_name: str = "Sustained Load"):
        """Run a sustained load test for a specified duration"""
        print(f"\n⏰ Starting Sustained Load Test: {test_name}")
        print(f"   Concurrent Users: {concurrent_users}")
        print(f"   Duration: {duration_seconds} seconds")
        
        endpoints = [
            ("/actuator/health", "GET", None),
            ("/api/v1/publishing/vendors/health", "GET", None),
        ]
        
        start_time = time.time()
        end_time = start_time + duration_seconds
        
        connector = aiohttp.TCPConnector(limit=concurrent_users, limit_per_host=concurrent_users)
        timeout = aiohttp.ClientTimeout(total=10)
        
        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            all_responses = []
            request_count = 0
            
            while time.time() < end_time:
                # Create batch of concurrent requests
                batch_tasks = []
                batch_size = min(concurrent_users, 50)  # Limit batch size
                
                for _ in range(batch_size):
                    endpoint, method, _ = random.choice(endpoints)
                    task = self.make_request(session, endpoint, method)
                    batch_tasks.append(task)
                
                # Execute batch
                batch_responses = await asyncio.gather(*batch_tasks, return_exceptions=True)
                all_responses.extend([r for r in batch_responses if not isinstance(r, Exception)])
                request_count += len(batch_responses)
                
                # Small delay between batches
                await asyncio.sleep(0.1)
        
        total_time = time.time() - start_time
        
        # Analyze sustained load results
        successful_requests = [r for r in all_responses if r["success"]]
        failed_requests = [r for r in all_responses if not r["success"]]
        
        response_times = [r["response_time_ms"] for r in successful_requests]
        
        if response_times:
            avg_response_time = statistics.mean(response_times)
            p95_response_time = statistics.quantiles(response_times, n=20)[18] if len(response_times) >= 20 else max(response_times)
        else:
            avg_response_time = p95_response_time = 0
        
        requests_per_second = request_count / total_time if total_time > 0 else 0
        
        print(f"   📊 Total Requests: {request_count}")
        print(f"   ✅ Success Rate: {len(successful_requests) / len(all_responses) * 100:.1f}%")
        print(f"   ❌ Failed Requests: {len(failed_requests)}")
        print(f"   📊 Throughput: {requests_per_second:.1f} req/sec")
        print(f"   ⏱️  Avg Response Time: {avg_response_time:.1f}ms")
        print(f"   📈 P95 Response Time: {p95_response_time:.1f}ms")
        print(f"   🕐 Total Time: {total_time:.2f}s")
    
    def print_summary(self):
        """Print a summary of all stress test results"""
        print("\n📊 STRESS TEST SUMMARY")
        print("=" * 80)
        
        for result in self.results:
            print(f"\n🔍 {result['test_name']}")
            print(f"   Endpoint: {result['method']} {result['endpoint']}")
            print(f"   Success Rate: {result['success_rate']:.1f}% ({result['successful_requests']}/{result['total_requests']})")
            print(f"   ❌ Failed: {result['failed_requests']}, 💥 Exceptions: {result['exceptions']}")
            print(f"   Throughput: {result['requests_per_second']:.1f} req/sec")
            print(f"   Avg Response Time: {result['avg_response_time_ms']:.1f}ms")
            print(f"   P95 Response Time: {result['p95_response_time_ms']:.1f}ms")
            print(f"   P99 Response Time: {result['p99_response_time_ms']:.1f}ms")
        
        # Save results to file
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"stress_test_results_{timestamp}.json"
        
        with open(filename, 'w') as f:
            json.dump(self.results, f, indent=2, default=str)
        
        print(f"\n💾 Results saved to: {filename}")

async def main():
    """Main function to run stress testing"""
    # Check if service is running
    import urllib.request
    try:
        urllib.request.urlopen("http://localhost:8083/actuator/health", timeout=5)
        print("✅ Publishing service is running on port 8083")
    except:
        print("❌ Publishing service is not running on port 8083")
        print("Please start the service first: mvn spring-boot:run")
        return
    
    # Initialize and run stress tests
    tester = PublishingServiceStressTester()
    await tester.run_comprehensive_stress_test()

if __name__ == "__main__":
    asyncio.run(main())
