from pymongo import MongoClient
import pandas as pd
import urllib.request, json, os, csv
connection = MongoClient()

db = connection.knowledge


items = db.Targets.find(no_cursor_timeout=True)
i = 0
err = 0
for it in items:
  if not "NCBIGeneURL" in it:
    continue
  os.system(f'scrapy runspider TargetCrawler.py -o output.csv -a url={it["NCBIGeneURL"]} -a gene={it["symbol"]}')
  if os.path.getsize('output.csv') < 5:
    os.remove("output.csv")
    continue
  df = pd.read_csv("output.csv")
  l = len(df)
  if i == 0:
    os.system('cp output.csv targets.csv')
  else:
    os.system(f'tail -{l} output.csv >> targets.csv') 
  os.remove("output.csv")
  i = i + 1

