from pathlib import Path

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_text_splitters import RecursiveCharacterTextSplitter


PMIDS_FILE = "pmids.txt"
QUESTION_FILE = "question.txt"
RESULT_FILE = "result.txt"

ARTICLES_DIR = Path(".")
CHROMA_DIR = Path("./chroma_db")


def load_article_text(pmid: str) -> str:
    return (ARTICLES_DIR / f"{pmid}.txt").read_text(encoding="utf-8")


def build_article_db(pmid: str, text: str, embeddings):
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=1200,
        chunk_overlap=200,
    )

    doc = Document(
        page_content=text,
        metadata={"pmid": pmid},
    )

    chunks = splitter.split_documents([doc])

    for i, chunk in enumerate(chunks):
        chunk.metadata["pmid"] = pmid
        chunk.metadata["chunk"] = i

    db = Chroma.from_documents(
        documents=chunks,
        embedding=embeddings,
        persist_directory=str(CHROMA_DIR / pmid),
        collection_name=f"article_{pmid}",
    )

    return db


def get_top_chunks_for_article(pmid: str, question: str, embeddings):
    text = load_article_text(pmid)
    db = build_article_db(pmid, text, embeddings)

    return db.similarity_search(
        question,
        k=5,
    )


def main():
    pmids = [
        line.strip()
        for line in Path(PMIDS_FILE).read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]

    question = Path(QUESTION_FILE).read_text(encoding="utf-8").strip()

    embeddings = OpenAIEmbeddings(
        model="text-embedding-3-small"
    )

    all_top_docs = []

    for pmid in pmids:
        top_docs = get_top_chunks_for_article(
            pmid=pmid,
            question=question,
            embeddings=embeddings,
        )
        all_top_docs.extend(top_docs)

    context = "\n\n---\n\n".join(
        f"[PMID: {doc.metadata['pmid']}, chunk: {doc.metadata.get('chunk')}]\n"
        f"{doc.page_content}"
        for doc in all_top_docs
    )

    llm = ChatOpenAI(
        model="gpt-4.1-mini",
        temperature=0,
    )

    response = llm.invoke(
        f"""Answer the question using only the article excerpts below.

Cite every factual claim using the PMID from the relevant excerpt, like this:
[PMID: 12345678]

Question:
{question}

Article excerpts:
{context}
"""
    )

    print(f"{response.content}")


if __name__ == "__main__":
    main()
