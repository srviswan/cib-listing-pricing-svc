#!/usr/bin/env python3
"""
Load Testing Script for Publishing Service
Tests various endpoints under different load conditions
"""

import asyncio
import aiohttp
import time
import statistics
import json
from datetime import datetime
from typing import List, Dict, Any
import random

class PublishingServiceLoadTester:
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
            "basketCode": f"BASKET_{basket_id}",
            "basketName": f"Test Basket {basket_id}",
            "basketType": "EQUITY",
            "baseCurrency": "USD",
            "totalWeight": 1.0,
            "constituents": constituents
        }
    
    def generate_price_data(self, basket_id: str) -> Dict[str, Any]:
        """Generate realistic price data for testing"""
        return {
            "basketId": basket_id,
            "price": round(random.uniform(100.0, 1000.0), 2),
            "currency": "USD",
            "timestamp": datetime.now().isoformat(),
            "source": "LOAD_TEST"
        }
    
    async def run_load_test(self, endpoint: str, method: str = "GET", 
                           concurrent_users: int = 10, total_requests: int = 100,
                           data_generator=None, test_name: str = None):
        """Run a load test for a specific endpoint"""
        print(f"\n🚀 Starting Load Test: {test_name or endpoint}")
        print(f"   Endpoint: {method} {endpoint}")
        print(f"   Concurrent Users: {concurrent_users}")
        print(f"   Total Requests: {total_requests}")
        
        start_time = time.time()
        
        async with aiohttp.ClientSession() as session:
            # Create tasks for concurrent requests
            tasks = []
            
            for i in range(total_requests):
                if data_generator:
                    data = data_generator(f"LOAD_TEST_{i:03d}")
                else:
                    data = None
                
                task = self.make_request(session, endpoint, method, data)
                tasks.append(task)
            
            # Execute all requests concurrently
            responses = await asyncio.gather(*tasks)
            
        end_time = time.time()
        total_time = end_time - start_time
        
        # Analyze results
        successful_requests = [r for r in responses if r["success"]]
        failed_requests = [r for r in responses if not r["success"]]
        
        response_times = [r["response_time_ms"] for r in successful_requests]
        
        if response_times:
            avg_response_time = statistics.mean(response_times)
            median_response_time = statistics.median(response_times)
            min_response_time = min(response_times)
            max_response_time = max(response_times)
            p95_response_time = statistics.quantiles(response_times, n=20)[18]  # 95th percentile
        else:
            avg_response_time = median_response_time = min_response_time = max_response_time = p95_response_time = 0
        
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
            "success_rate": len(successful_requests) / total_requests * 100,
            "total_time_seconds": total_time,
            "requests_per_second": requests_per_second,
            "avg_response_time_ms": avg_response_time,
            "median_response_time_ms": median_response_time,
            "min_response_time_ms": min_response_time,
            "max_response_time_ms": max_response_time,
            "p95_response_time_ms": p95_response_time,
            "responses": responses
        }
        
        self.results.append(test_result)
        
        # Print results
        print(f"   ✅ Success Rate: {test_result['success_rate']:.1f}%")
        print(f"   📊 Throughput: {test_result['requests_per_second']:.1f} req/sec")
        print(f"   ⏱️  Avg Response Time: {test_result['avg_response_time_ms']:.1f}ms")
        print(f"   📈 P95 Response Time: {test_result['p95_response_time_ms']:.1f}ms")
        print(f"   🕐 Total Time: {test_result['total_time_seconds']:.2f}s")
        
        return test_result
    
    async def run_comprehensive_load_test(self):
        """Run comprehensive load testing across all endpoints"""
        print("🔥 Starting Comprehensive Load Testing for Publishing Service")
        print("=" * 80)
        
        # Test 1: Health endpoint - Light load
        await self.run_load_test(
            endpoint="/actuator/health",
            method="GET",
            concurrent_users=5,
            total_requests=50,
            test_name="Health Check - Light Load"
        )
        
        # Test 2: Health endpoint - Medium load
        await self.run_load_test(
            endpoint="/actuator/health",
            method="GET",
            concurrent_users=20,
            total_requests=200,
            test_name="Health Check - Medium Load"
        )
        
        # Test 3: Vendor health endpoint - Light load
        await self.run_load_test(
            endpoint="/api/v1/publishing/vendors/health",
            method="GET",
            concurrent_users=5,
            total_requests=50,
            test_name="Vendor Health - Light Load"
        )
        
        # Test 4: Vendor health endpoint - Medium load
        await self.run_load_test(
            endpoint="/api/v1/publishing/vendors/health",
            method="GET",
            concurrent_users=20,
            total_requests=200,
            test_name="Vendor Health - Medium Load"
        )
        
        # Test 5: Basket listing - Light load
        await self.run_load_test(
            endpoint="/api/v1/publishing/basket/LOAD_TEST_001/list",
            method="POST",
            concurrent_users=3,
            total_requests=20,
            data_generator=self.generate_basket_data,
            test_name="Basket Listing - Light Load"
        )
        
        # Test 6: Basket listing - Medium load
        await self.run_load_test(
            endpoint="/api/v1/publishing/basket/LOAD_TEST_002/list",
            method="POST",
            concurrent_users=10,
            total_requests=50,
            data_generator=self.generate_basket_data,
            test_name="Basket Listing - Medium Load"
        )
        
        # Test 7: Price publishing - Light load
        await self.run_load_test(
            endpoint="/api/v1/publishing/basket/LOAD_TEST_003/price",
            method="POST",
            concurrent_users=3,
            total_requests=20,
            data_generator=self.generate_price_data,
            test_name="Price Publishing - Light Load"
        )
        
        # Test 8: Price publishing - Medium load
        await self.run_load_test(
            endpoint="/api/v1/publishing/basket/LOAD_TEST_004/price",
            method="POST",
            concurrent_users=10,
            total_requests=50,
            data_generator=self.generate_price_data,
            test_name="Price Publishing - Medium Load"
        )
        
        # Test 9: Mixed endpoints - High load
        await self.run_mixed_load_test(
            concurrent_users=30,
            total_requests=300,
            test_name="Mixed Endpoints - High Load"
        )
        
        print("\n" + "=" * 80)
        print("🎯 Load Testing Complete!")
        self.print_summary()
    
    async def run_mixed_load_test(self, concurrent_users: int = 30, 
                                 total_requests: int = 300, test_name: str = "Mixed Load"):
        """Run a mixed load test across multiple endpoints"""
        print(f"\n🚀 Starting Mixed Load Test: {test_name}")
        print(f"   Concurrent Users: {concurrent_users}")
        print(f"   Total Requests: {total_requests}")
        
        endpoints = [
            ("/actuator/health", "GET", None),
            ("/api/v1/publishing/vendors/health", "GET", None),
            ("/api/v1/publishing/basket/MIXED_TEST_001/list", "POST", self.generate_basket_data),
            ("/api/v1/publishing/basket/MIXED_TEST_002/price", "POST", self.generate_price_data),
        ]
        
        start_time = time.time()
        
        async with aiohttp.ClientSession() as session:
            tasks = []
            
            for i in range(total_requests):
                endpoint, method, data_gen = random.choice(endpoints)
                
                if data_gen:
                    data = data_gen(f"MIXED_{i:03d}")
                else:
                    data = None
                
                task = self.make_request(session, endpoint, method, data)
                tasks.append(task)
            
            responses = await asyncio.gather(*tasks)
        
        end_time = time.time()
        total_time = end_time - start_time
        
        # Analyze mixed results
        successful_requests = [r for r in responses if r["success"]]
        failed_requests = [r for r in responses if not r["success"]]
        
        response_times = [r["response_time_ms"] for r in successful_requests]
        
        if response_times:
            avg_response_time = statistics.mean(response_times)
            p95_response_time = statistics.quantiles(response_times, n=20)[18]
        else:
            avg_response_time = p95_response_time = 0
        
        requests_per_second = total_requests / total_time if total_time > 0 else 0
        
        print(f"   ✅ Success Rate: {len(successful_requests) / total_requests * 100:.1f}%")
        print(f"   📊 Throughput: {requests_per_second:.1f} req/sec")
        print(f"   ⏱️  Avg Response Time: {avg_response_time:.1f}ms")
        print(f"   📈 P95 Response Time: {p95_response_time:.1f}ms")
        print(f"   🕐 Total Time: {total_time:.2f}s")
    
    def print_summary(self):
        """Print a summary of all load test results"""
        print("\n📊 LOAD TEST SUMMARY")
        print("=" * 80)
        
        for result in self.results:
            print(f"\n🔍 {result['test_name']}")
            print(f"   Endpoint: {result['method']} {result['endpoint']}")
            print(f"   Success Rate: {result['success_rate']:.1f}% ({result['successful_requests']}/{result['total_requests']})")
            print(f"   Throughput: {result['requests_per_second']:.1f} req/sec")
            print(f"   Avg Response Time: {result['avg_response_time_ms']:.1f}ms")
            print(f"   P95 Response Time: {result['p95_response_time_ms']:.1f}ms")
        
        # Save results to file
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"load_test_results_{timestamp}.json"
        
        with open(filename, 'w') as f:
            json.dump(self.results, f, indent=2, default=str)
        
        print(f"\n💾 Results saved to: {filename}")

async def main():
    """Main function to run load testing"""
    # Check if service is running
    import urllib.request
    try:
        urllib.request.urlopen("http://localhost:8083/actuator/health", timeout=5)
        print("✅ Publishing service is running on port 8083")
    except:
        print("❌ Publishing service is not running on port 8083")
        print("Please start the service first: mvn spring-boot:run")
        return
    
    # Initialize and run load tests
    tester = PublishingServiceLoadTester()
    await tester.run_comprehensive_load_test()

if __name__ == "__main__":
    asyncio.run(main())
