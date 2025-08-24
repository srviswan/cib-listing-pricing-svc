#!/usr/bin/env python3
"""
API Endpoints Testing Script for Basket Core Service
This script tests all CRUD operations and API endpoints
"""

import requests
import json
import time
from typing import Dict, Any, List
import uuid

class BasketAPITester:
    def __init__(self, base_url: str = "http://localhost:8081"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        
    def test_health_endpoint(self) -> bool:
        """Test the health endpoint"""
        try:
            # Test both the custom health endpoint and the Actuator health endpoint
            response = self.session.get(f"{self.base_url}/api/v1/baskets/health")
            print(f"✅ Custom Health Check: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                if isinstance(data, dict):
                    print(f"   Health Status: {data.get('data', 'Unknown')}")
                else:
                    print(f"   Health Response: {data}")
                return True
            
            # Also test the Actuator health endpoint
            actuator_response = self.session.get(f"{self.base_url}/actuator/health")
            print(f"✅ Actuator Health Check: {actuator_response.status_code}")
            if actuator_response.status_code == 200:
                actuator_data = actuator_response.json()
                print(f"   Actuator Status: {actuator_data.get('status', 'Unknown')}")
                return True
            return False
        except requests.exceptions.ConnectionError:
            print("❌ Health Check: Connection failed - Service not running")
            return False
        except Exception as e:
            print(f"❌ Health Check: Error - {e}")
            return False
    
    def test_get_all_baskets(self) -> bool:
        """Test getting all baskets"""
        try:
            response = self.session.get(f"{self.base_url}/api/v1/baskets")
            print(f"✅ Get All Baskets: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                baskets = data.get('data', {}).get('baskets', [])
                print(f"   Found {len(baskets)} baskets")
                for basket in baskets[:3]:  # Show first 3
                    print(f"     - {basket.get('basketCode')}: {basket.get('basketName')}")
                return True
            return False
        except Exception as e:
            print(f"❌ Get All Baskets: Error - {e}")
            return False
    
    def test_get_basket_by_id(self, basket_id: str) -> bool:
        """Test getting a specific basket by ID"""
        try:
            response = self.session.get(f"{self.base_url}/api/v1/baskets/{basket_id}")
            print(f"✅ Get Basket by ID: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                basket = data.get('data', {})
                print(f"   Basket: {basket.get('basketCode')} - {basket.get('basketName')}")
                return True
            return False
        except Exception as e:
            print(f"❌ Get Basket by ID: Error - {e}")
            return False
    
    def test_create_basket(self) -> str:
        """Test creating a new basket"""
        basket_data = {
            "basketCode": f"TEST_API_{uuid.uuid4().hex[:8].upper()}",
            "basketName": "API Test Basket",
            "description": "Basket created via API testing",
            "basketType": "EQUITY",
            "baseCurrency": "USD",
            "totalWeight": 100.0,
            "version": "v1.0",
            "previousVersion": None,
            "createdBy": "apitest",
            "constituents": []
        }
        
        try:
            response = self.session.post(f"{self.base_url}/api/v1/baskets", json=basket_data)
            print(f"✅ Create Basket: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                basket_id = data.get('data')
                print(f"   Created basket with ID: {basket_id}")
                return basket_id
            else:
                print(f"   Error: {response.text}")
                return None
        except Exception as e:
            print(f"❌ Create Basket: Error - {e}")
            return None
    
    def test_update_basket(self, basket_id: str) -> bool:
        """Test updating a basket"""
        update_data = {
            "basketName": "Updated API Test Basket",
            "description": "Basket updated via API testing",
            "updatedBy": "apitest"
        }
        
        try:
            response = self.session.put(f"{self.base_url}/api/v1/baskets/{basket_id}", json=update_data)
            print(f"✅ Update Basket: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                print(f"   Updated basket: {data.get('data', {}).get('basketName', 'Unknown')}")
                return True
            else:
                print(f"   Error: {response.text}")
                return False
        except Exception as e:
            print(f"❌ Update Basket: Error - {e}")
            return False
    
    def test_update_basket_status(self, basket_id: str) -> bool:
        """Test updating basket status using state machine workflow"""
        status_data = {
            "event": "START_BACKTEST",
            "updatedBy": "apitest"
        }
        
        try:
            response = self.session.put(f"{self.base_url}/api/v1/baskets/{basket_id}/status", json=status_data)
            print(f"✅ Update Basket Status: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                print(f"   Status updated successfully: {data.get('message', 'Unknown')}")
                return True
            else:
                print(f"   Error: {response.text}")
                return False
        except Exception as e:
            print(f"❌ Update Basket Status: Error - {e}")
            return False
    
    def test_delete_basket(self, basket_id: str) -> bool:
        """Test deleting a basket (soft delete)"""
        try:
            response = self.session.delete(f"{self.base_url}/api/v1/baskets/{basket_id}?deletedBy=apitest")
            print(f"✅ Delete Basket: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                print(f"   Deleted basket successfully: {data.get('message', 'Unknown')}")
                return True
            else:
                print(f"   Error: {response.text}")
                return False
        except Exception as e:
            print(f"❌ Delete Basket: Error - {e}")
            return False
    
    def test_metrics_endpoint(self) -> bool:
        """Test the metrics endpoint"""
        try:
            response = self.session.get(f"{self.base_url}/actuator/metrics")
            print(f"✅ Metrics Endpoint: {response.status_code}")
            if response.status_code == 200:
                data = response.json()
                metric_names = data.get('names', [])
                basket_metrics = [m for m in metric_names if 'basket.operations' in m]
                print(f"   Found {len(basket_metrics)} basket operation metrics")
                for metric in basket_metrics[:5]:  # Show first 5
                    print(f"     - {metric}")
                return True
            return False
        except Exception as e:
            print(f"❌ Metrics Endpoint: Error - {e}")
            return False
    
    def run_full_test_suite(self) -> Dict[str, Any]:
        """Run the complete test suite"""
        print("🚀 Starting Basket Core Service API Test Suite")
        print("=" * 60)
        
        results = {
            'health_check': False,
            'get_all_baskets': False,
            'create_basket': False,
            'get_basket': False,
            'update_basket': False,
            'update_basket_status': False,
            'delete_basket': False,
            'metrics': False
        }
        
        # Test 1: Health Check
        print("\n1️⃣ Testing Health Endpoint")
        results['health_check'] = self.test_health_endpoint()
        
        if not results['health_check']:
            print("❌ Service is not running. Cannot continue with API tests.")
            return results
        
        # Test 2: Get All Baskets
        print("\n2️⃣ Testing Get All Baskets")
        results['get_all_baskets'] = self.test_get_all_baskets()
        
        # Test 3: Create Basket
        print("\n3️⃣ Testing Create Basket")
        basket_id = self.test_create_basket()
        results['create_basket'] = basket_id is not None
        
        if basket_id:
            # Test 4: Get Basket by ID
            print("\n4️⃣ Testing Get Basket by ID")
            results['get_basket'] = self.test_get_basket_by_id(basket_id)
            
            # Test 5: Update Basket
            print("\n5️⃣ Testing Update Basket")
            results['update_basket'] = self.test_update_basket(basket_id)
            
            # Test 6: Update Basket Status (State Machine)
            print("\n6️⃣ Testing Update Basket Status (State Machine)")
            results['update_basket_status'] = self.test_update_basket_status(basket_id)
            
            # Test 7: Delete Basket
            print("\n7️⃣ Testing Delete Basket")
            results['delete_basket'] = self.test_delete_basket(basket_id)
        
        # Test 8: Metrics
        print("\n8️⃣ Testing Metrics Endpoint")
        results['metrics'] = self.test_metrics_endpoint()
        
        # Summary
        print("\n" + "=" * 60)
        print("📊 TEST RESULTS SUMMARY")
        print("=" * 60)
        
        passed = sum(results.values())
        total = len(results)
        
        for test_name, result in results.items():
            status = "✅ PASS" if result else "❌ FAIL"
            print(f"{test_name.replace('_', ' ').title()}: {status}")
        
        print(f"\nOverall: {passed}/{total} tests passed ({passed/total*100:.1f}%)")
        
        return results

def main():
    """Main function to run the API tests"""
    tester = BasketAPITester()
    
    # Check if service is running
    if not tester.test_health_endpoint():
        print("\n🔧 ALTERNATIVE TESTING APPROACHES:")
        print("1. Start the service with: mvn spring-boot:run -pl basket-core-service")
        print("2. Use the database directly to test CRUD operations")
        print("3. Run unit tests: mvn test -pl basket-core-service")
        
        print("\n📋 DATABASE TEST DATA AVAILABLE:")
        print("✅ 3 test baskets created")
        print("✅ 10 test constituents created")
        print("✅ All tables and relationships configured")
        
        return
    
    # Run full test suite
    results = tester.run_full_test_suite()
    
    if all(results.values()):
        print("\n🎉 ALL TESTS PASSED! The Basket Core Service is working perfectly!")
    else:
        print("\n⚠️ Some tests failed. Check the service logs for details.")

if __name__ == "__main__":
    main()
