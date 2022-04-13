import requests
import xml.etree.ElementTree as ET
import sys, os
from utils import grouper
from math import ceil

def ask(query, max = 50000):
    pub_ids = get_pub_ids(query, max)

    pubs = []
    chunk_size = 200
    step_size = ceil(len(pub_ids)/chunk_size)

    for i, g in enumerate(grouper(pub_ids, chunk_size)):
        print(i, step_size)
        get_pub_content([id for id in g if id], i == step_size-1)

    return pubs


def get_pub_ids(query, max = 50000):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term={query}&retmax={max}&sort=relevance"
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
                #os.system("gtail -n +4 myfile.xml >> pubmed.xml")
            else:
                result = text[text.index(b'<PubmedArticle>'):text.index(b'</PubmedArticleSet>')]
                #os.system("gtail -n +4 myfile.xml| ghead -n -2 >> pubmed.xml")
        else:
            if done:
                pass
                #os.system("mv myfile.xml pubmed.xml")
            else:
                result = text[:text.index(b'</PubmedArticleSet>')]
            #os.system("ghead -n -1 myfile.xml > pubmed.xml")
    except:
        print("ERROR")
    file1 = open("pubmed.xml", "ab+")
    file1.write(result)
    file1.close()


def get_str_id_content(idstr):
    res = requests.get(
        url=f"https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&id={idstr}&rettype=text"
    )
    text = res.content
    
    file1 = open("myfile.xml", "wb")
    file1.write(res.content)
    file1.close()
    if os.path.exists("pubmed.xml"):
        result = text[text.index("<PubmedArticle>"):text.index("</PubmedArticleSet>")]
        os.system("gtail -n +4 myfile.xml| ghead -n -2 >> pubmed.xml")
    else:
        result = text[0:text.index("</PubmedArticleSet>")]
        os.system("ghead -n -1 myfile.xml > pubmed.xml")

if __name__ == '__main__':
    max = 50000
    if os.path.exists("pubmed.xml"):
        os.remove("pubmed.xml")
    if len(sys.argv) > 2:
        max = int(sys.argv[1])
        term = ""
        for i in range(2, len(sys.argv)):
            if term == "":
                term = term + sys.argv[i]
            else:
                term = term + " " + sys.argv[i]
        print(f'Max number of articles is {max}')
        print(f'Searching for "{term}"')
        ask(term, max)
    else:
        print('Arg1 = number of articles. Arg2...ArgN = provide query')

