import requests
import xml.etree.ElementTree as ET
import sys, os
from utils import grouper
from math import ceil

def ask(query, max = 50000, xmlfile = "pubmed.xml"):
    pub_ids = get_pub_ids(query, max)

    pubs = []
    chunk_size = 200
    step_size = ceil(len(pub_ids)/chunk_size)

    for i, g in enumerate(grouper(pub_ids, chunk_size)):
        print(i, step_size)
        get_pub_content([id for id in g if id], i == step_size-1, xmlfile)

    return pubs


def get_pub_ids(query, max = 50000):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term={query}&retmax={max}&sort=date"
    )

    res.raise_for_status()

    return [f.text for f in ET.fromstring(res.content).find('IdList')]

def tree(root):
    for child in root:
      tree(child)
      print(child.tag, child.attrib, child.text)

def get_pub_content(ids, done=False, xmlfile = "pubmed.xml"):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={','.join(ids)}&rettype=text"
    )
    text = res.text
    result = ''
    #print(text)
    try:
        if os.path.exists(xmlfile):
            if done:
                result = text[text.index('<PubmedArticle>'):]
            else:
                result = text[text.index('<PubmedArticle>'):text.index('</PubmedArticleSet>')]
        else:
            if done:
                result = text
            else:
                result = text[:text.index('</PubmedArticleSet>')]
    except:
        print("ERROR")
    file1 = open(xmlfile, "a+")
    file1.write(result)
    file1.close()

if __name__ == '__main__':
    max = 50000
    if len(sys.argv) > 3:
        max = int(sys.argv[1])
        xmlfile = sys.argv[2]
        term = ""
        for i in range(3, len(sys.argv)):
            if term == "":
                term = term + sys.argv[i]
            else:
                term = term + " " + sys.argv[i]
        print(f'Max number of articles is {max}')
        print(f'Searching for "{term}"')
        print(f'Using XMLfile {xmlfile}')
        if os.path.exists(xmlfile):
          os.remove(xmlfile)
        ask(term, max, xmlfile)
    else:
        print('Arg1 = number of articles. Arg2 = XML file, Arg3...ArgN = provide query')

