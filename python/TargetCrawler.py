import scrapy

def build_base():
    return {"gene": "", "targetSymbol": ""}


class AbSpider(scrapy.Spider):
    name = 'NCBIspider'
    allowed_domains = ['https://www.ncbi.nlm.nih.gov']
    def __init__(self, url='https://www.ncbi.nlm.nih.gov', gene='A1BG', **kwargs):
        self.start_urls = [url]
        self.gene = gene
        #self.download_delay = 0.05
        super().__init__(**kwargs)
    def parse(self, response):
        result = response.xpath('//span[@class = "gn"]/text()').getall()
        if result:
            answer = build_base()
            answer["gene"] = self.gene
            answer["targetSymbol"] = result
            yield answer

