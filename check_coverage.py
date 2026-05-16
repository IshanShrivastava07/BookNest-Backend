import csv
import os

modules = [
    "api-gateway", "auth-service", "book-service", "cart-service", 
    "notification-service", "order-service", "review-service", 
    "wallet-service", "wishlist-service"
]

for m in modules:
    csv_path = os.path.join(m, "target", "site", "jacoco", "jacoco.csv")
    if not os.path.exists(csv_path):
        print(f"{m}: CSV NOT FOUND")
        continue
    
    missed = 0
    covered = 0
    with open(csv_path, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            missed += int(row['INSTRUCTION_MISSED'])
            covered += int(row['INSTRUCTION_COVERED'])
    
    total = missed + covered
    percentage = (covered / total * 100) if total > 0 else 0
    print(f"{m}: {percentage:.2f}% ({covered}/{total})")
