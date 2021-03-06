import os
import sys
import xml.etree.ElementTree as ET
from math import ceil

import requests

from utils import grouper


def ask(query):
    pub_ids = get_pub_ids(query)

    pubs = []
    chunk_size = 200
    step_size = ceil(len(pub_ids)/chunk_size)

    for i, g in enumerate(grouper(pub_ids, chunk_size)):
        print(i, step_size)
        get_pub_content([id for id in g if id], i == step_size-1)

    return pubs


def get_pub_ids(query):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term={query}&retmax=25000&sort=relevance"
    )

    res.raise_for_status()

    return [f.text for f in ET.fromstring(res.content).find('IdList')]


def tree(root):
    for child in root:
      tree(child)
      print(child.tag, child.attrib, child.text)


def get_pub_content(ids, done=False):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={','.join(ids)}&rettype=text"
    )
    text = res.content
    result = b''
    #print(text)
    try:
        if os.path.exists("pubmed.xml"):
            if done:
                result = text[text.index(b'<PubmedArticle>'):]
            else:
                result = text[text.index(b'<PubmedArticle>'):text.index(b'</PubmedArticleSet>')]
        else:
            if done:
                pass
            else:
                result = text[:text.index(b'</PubmedArticleSet>')]
    except:
        print("ERROR")
    file1 = open("pubmed.xml", "ab+")
    file1.write(result)
    file1.close()

if __name__ == '__main__':
    if os.path.exists("pubmed.xml"):
        os.remove("pubmed.xml")
    if len(sys.argv) > 1:
        term = ""
        for i in range(1, len(sys.argv)):
            if term == "":
                term = term + sys.argv[i]
            else:
                term = term + " " + sys.argv[i]
        ask(term)
    else:
        print('Provide multiple arguments (query)')

