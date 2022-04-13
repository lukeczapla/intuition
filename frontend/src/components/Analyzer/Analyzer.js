
import React, {useEffect, useState, createRef} from 'react';
import Select from 'react-select';
import bsCustomFileInput from 'bs-custom-file-input';
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import endpoint from '../../endpoint';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import FloatingLabel from 'react-bootstrap/FloatingLabel';
import ListGroup from 'react-bootstrap/ListGroup';
import Modal from "react-bootstrap/Modal";


function JobModal(props) {
    return (
        <Modal {...props} size="lg" aria-labelledby="contained-modal-title-vcenter" centered>
            <Modal.Header closeButton>
                <Modal.Title id="contained-modal-title-vcenter">
                    Job is Submitted {props.jobName !== "" && " - Your job has code " + props.jobName}
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <h4>Your Calculation is Running</h4>
                <p>
                    You can check on the progress of the process under the Resources tab.  Curating a single item may finish
                    right away, but running all terms under a key with many items can take an hour or two to finish.
                </p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={props.onHide}>Close</Button>
            </Modal.Footer>
        </Modal>
    );
}


const Analyzer = (props) => {

    const choose = (a, b) => {
        if (props.state == null) return b;
        return props.state[a];
    }

    const [variant, setVariant] = useState(choose("variant", null));
    const [variants, setVariants] = useState(choose("variants", []));
    const [variantKeys, setVariantKeys] = useState(choose("variantKeys", []));
    const [variantSelected, setVariantSelected] = useState(choose("variantSelected", ""));
    const [geneName, setGeneName] = useState(choose("geneName", ""));
    const [cancerName, setCancerName] = useState(choose("cancerName", ""));
    const [drugs, setDrugs] = useState(choose("drugs", []));
    const [variantKeySelected, setVariantKeySelected] = useState(choose("variantKeySelected", "none"));
    const [articleItem, setArticleItem] = useState(choose("articleItem", null));
    const [articleRecords, setArticleRecords] = useState(choose("articleRecords", []));
    const [article, setArticle] = useState(choose("article", ""));
    const [articleList, setArticleList] = useState(choose("articleList", []));
    const [articleList2, setArticleList2] = useState(choose("articleList2"), []);
    const [topArticles, setTopArticles] = useState(choose("topArticles", 5));
    const [textAnalysis, setTextAnalysis] = useState(choose("textAnalysis", null));
    const [textView, setTextView] = useState(choose("textView", "Sentence Island"));
    const [innerHTML, setInnerHTML] = useState(choose("innerHTML", ""));
    const [innerHTML2, setInnerHTML2] = useState(choose("innerHTML2", ""));
    const [processing, setProcessing] = useState(choose("processing", false));
    const [uploadFiles, setUploadFiles] = useState(choose("uploadFiles", null));
    const [createVariantKey, setCreateVariantKey] = useState(choose("createVariantKey", ""));
    const [submitRun, setSubmitRun] = useState(choose("submitRun", false));
    const [tier, setTier] = useState(choose("tier", 0));
    const [noteBox, setNoteBox] = useState(choose("noteBox", ""));
    const [excluded, setExcluded] = useState(choose("excluded", false));
    const [textPages, setTextPages] = useState(choose("textPages", 0));
    const [articlesTier1, setArticlesTier1] = useState(choose("articlesTier1"), []);
    const [articlesTier2, setArticlesTier2] = useState(choose("articlesTier2"), []);
    const [total, setTotal] = useState(choose("total"), 0);
    const [key, setKey] = useState(choose("key"), "alteration");

    const [state, setState] = useState({
        variant: variant, variants: variants, variantKeys: variantKeys, variantSelected: variantSelected, geneName: geneName, cancerName: cancerName, drugs: drugs,
        variantKeySelected: variantKeySelected, articleItem: articleItem, articleRecords: articleRecords, article: article, articleList: articleList, articleList2 : articleList2,
        topArticles: topArticles, textAnalysis: textAnalysis, textView: textView, innerHTML: innerHTML, innerHTML2: innerHTML2, processing: processing, uploadFiles: uploadFiles,
        createVariantKey: createVariantKey, submitRun: submitRun, noteBox: noteBox, excluded: excluded, textPages: textPages, tier: tier,
        articlesTier1: articlesTier1, articlesTier2: articlesTier2, total: total, key: key
    });

    const [jobName, setJobName] = useState("");
    const [jobModalShow, setJobModalShow] = useState(false);
    const formRef = createRef();

    const tiers = ["Most Relevant", "Possibly Relevant", "All Articles"];

    useEffect(() => {

        fetch(endpoint + '/getVariantKeys').then(result => result.json()).then(data => {
            setVariantKeys(data);
        });
        fetch(endpoint +'/getAllVariants').then(result => result.json()).then(data => {
            setVariants(data);
        });
        bsCustomFileInput.init();
        return () => {
            let v = {variant: variant, variants: variants, variantKeys: variantKeys, variantSelected: variantSelected, geneName: geneName, cancerName: cancerName, drugs: drugs,
                variantKeySelected: variantKeySelected, articleItem: articleItem, articleRecords: articleRecords, article: article, articleList: articleList,
                articleList2: articleList2,
                topArticles: topArticles, textAnalysis: textAnalysis, textView: textView, innerHTML: innerHTML, innerHTML2: innerHTML2, processing: processing, uploadFiles: uploadFiles,
                createVariantKey: createVariantKey, submitRun: submitRun, noteBox: noteBox, excluded: excluded, textPages: textPages, tier: tier,
                articlesTier1: articlesTier1, articlesTier2: articlesTier2, total: total, key: key};
            props.push(v);
            //console.log("Leaving analyzer");
        }

    }, []);

    useEffect(() => {
        if (props.state == null) return;
        setState(props.state);
        setVariant(props.state.variant);
        setVariants(props.state.variants);
        setVariantKeys(props.state.variantKeys);
        setVariantSelected(props.state.variantSelected);
        setGeneName(props.state.geneName);
        setCancerName(props.state.cancerName);
        setDrugs(props.state.drugs);
        setVariantKeySelected(props.state.variantKeySelected);
        setArticleItem(props.state.articleItem);
        setArticleRecords(props.state.articleRecords);
        setArticle(props.state.article);
        setArticleList(props.state.articleList);
        setArticleList2(props.state.articleList2);
        setTopArticles(props.state.topArticles);
        setTextAnalysis(props.state.textAnalysis);
        setTextView(props.state.textView);
        setInnerHTML(props.state.innerHTML);
        setInnerHTML2(props.state.innerHTML2);
        setProcessing(props.state.processing);
        setUploadFiles(props.state.uploadFiles);
        setCreateVariantKey(props.state.createVariantKey);
        setSubmitRun(props.state.submitRun);
        setNoteBox(props.state.noteBox);
        setExcluded(props.state.excluded);
        setTextPages(props.state.textPages);
        setTier(props.state.tier);
        setArticlesTier1(props.state.articlesTier1);
        setArticlesTier2(props.state.articlesTier2);
        setTotal(props.state.total);
        setKey(props.state.key);
    }, [props.state]);

    useEffect(() => {
        let v = {variant: variant, variants: variants, variantKeys: variantKeys, variantSelected: variantSelected, geneName: geneName, cancerName: cancerName, drugs: drugs,
            variantKeySelected: variantKeySelected, articleItem: articleItem, articleRecords: articleRecords, article: article, articleList: articleList,
            topArticles: topArticles, textAnalysis: textAnalysis, textView: textView, innerHTML: innerHTML, innerHTML2: innerHTML2, processing: processing, uploadFiles: uploadFiles,
            createVariantKey: createVariantKey, submitRun: submitRun, noteBox: noteBox, excluded: excluded, textPages: textPages, tier: tier,
            articlesTier1: articlesTier1, articlesTier2: articlesTier2, total: total, key: key};
        props.push(v);
        fetch(endpoint + '/getVariantKeys').then(result => result.json()).then(data => {
            setVariantKeys(data);
        });
        fetch(endpoint +'/getAllVariants').then(result => result.json()).then(data => {
            setVariants(data);
        });
    }, [props.flash]);

    const reloadVariants = () => {
        fetch(endpoint + '/getVariantKeys').then(result => result.json()).then(data => {
            setVariantKeys(data);
        });
        fetch(endpoint +'/getAllVariants').then(result => result.json()).then(data => {
            setVariants(data);
        });
    }

    const reactSelect = (e) => {
        setVariantSelected(e.value);
        setGeneName(e.value.split(":")[0]);
        if (e.value.split(":")[2] !== '') setCancerName(e.value.split(":")[2]);
        if (e.value.split(":")[3] !== '') setDrugs(e.value.split(":")[3].split(", "));
        //console.log(e.value);
        setArticleList([]);
        setArticleList2([]);
        setArticleRecords([]);
        setTextAnalysis(null);
        loadVariant(e.value);
    }

    const changeEvent = (e) => {
        //console.log("Change event");
        if (e.target.name === "variantSelected") {
            setVariantSelected(e.target.value);
            setGeneName(e.target.value.split(":")[0]);
            //console.log(e.target.value.split(":"));
            if (e.target.value.split(":")[2] !== '') setCancerName(e.target.value.split(":")[2]);
            if (e.target.value.split(":")[3] !== '') setDrugs(e.target.value.split(":")[3].split(", "));
            setArticleList([]);
            setArticleList2([]);
            setArticleRecords([]);
            setTextAnalysis(null);

            loadVariant(e.target.value);
        }
        else if (e.target.name === 'variantKeySelected') {
            setVariantKeySelected(e.target.value);
            setVariantSelected("");
        }
        else if (e.target.name === 'topArticles') {
            setTopArticles(e.target.value);
        }
        else if (e.target.name === 'textView') {
            setTextView(e.target.value);
            setInnerHTML(renderText(e.target.value));
        }
    }

    const getVariantOptions = () => {
        if (variants.length > 0 && variantKeySelected === "none") return variants.map((v,i) => ({"value": v.descriptor, "label": v.descriptor + (v.total !== 0 && v.articlesTier1 != null && v.articlesTier1.length > 0 ? " (" + v.total + " found articles)" : " (not run/no articles)")}));
        else if (variants.length > 0) return variants.filter(s => s.key === variantKeySelected).map((v,i) => ({"value": v.descriptor, "label": v.descriptor + (v.total !== 0 && v.articlesTier1 != null && v.articlesTier1.length > 0 ? " (" + v.total + " found articles)" : " (not run/no articles)")}));
    }

    const getVariantItems = () => {
        if (variants.length > 0 && variantKeySelected === "none") return variants.map((v,i) => <option value={v.descriptor} key={`${i}`}>{v.descriptor + (v.articlesTier1 != null && v.articlesTier1.length > 0 ? " (" + v.total + " found articles)" : " (not run/no articles)")}</option>);
        else if (variants.length > 0) return variants.filter(s => s.key === variantKeySelected).map((v,i) => <option value={v.descriptor} key={`${i}`}>{v.descriptor + (v.articlesTier1 != null && v.articlesTier1.length > 0 ? " (" + v.total + " found articles)" : " (not run/no articles)")}</option>);
    }

    const getVariantKeys = () => {
        if (variantKeys.length > 0) return variantKeys.map((k,i) => <option value={k} key={`${i}`}>{k}</option>);
    }

    const updateVariant = () => {
        if (article !== "" && variantSelected !== "" && variant !== null) {
            let v = variants.filter(s => s.descriptor === variantSelected)[0];
            //console.log(v);
            //console.log(excluded);
            if (excluded) {
                if (v.excludedPMIds === null || v.excludedPMIds === "") v.excludedPMIds = article;
                else {
                    let pmids = v.excludedPMIds.split(", ");
                    if (!pmids.includes(article)) v.excludedPMIds += ", " + article;
                }
            } else {
                if (v.excludedPMIds !== null && v.excludedPMIds.length > 3) {
                    let pmids = v.excludedPMIds.split(", ");
                    let result = "";
                    for (let i = 0; i < pmids.length; i++) {
                        if (pmids[i] !== article) {
                            if (result === "") result += pmids[i];
                            else result += ", " + pmids[i];
                        }
                    }
                    v.excludedPMIds = result;
                }
            }
            v.notes = noteBox;
            fetch(endpoint + "/updateVariant", {method: "POST", headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(v)})
                .then(response => response.text()).then(data => {
                    //console.log(data);
                }).catch(error => console.error(error));
        }
    }

    // updated for new information
    const updateList = (tier2 = false) => {
        let v1 = variant;
        let list = [];
        if (!tier2) {
            if (v1.articlesTier1 == null || v1.articlesTier1.length === 0) return;
            list = v1.articlesTier1;
        } else {
            if (v1.articlesTier2 == null || v1.articlesTier2.length === 0) return;
            list = v1.articlesTier2;
        }
        if (v1.excludedPMIds !== null) {
            let exclusionList = v1.excludedPMIds.split(", ");
            let removed = [];
            for (let i = 0; i < exclusionList.length; i++)
                if (list.indexOf(exclusionList[i]) >= 0) {
                    removed = list.splice(list.indexOf(exclusionList[i]), 1);
                    if (removed.length > 0) removed.forEach(element => list.push("[excluded] " + element));
                }
        }
        if (!tier2) setArticleList(list);
        else setArticleList2(list);
    }

    const updateTextAnalysis = (n) => {
        setTextPages(n);
        setInnerHTML(renderText(textView, textAnalysis[n]));
        setInnerHTML2(renderText2(textAnalysis[n]));
    }

    const loadArticle = (a, tier2 = false) => {
        updateVariant();
        updateList(tier2);
        setArticle(a);
        setTextPages(0);
        if (variant.excludedPMIds !== null && variant.excludedPMIds.length > 3) {
            let pmids = variant.excludedPMIds.split(", ");
            if (pmids.includes(a)) setExcluded(true);
            else setExcluded(false);
        } else setExcluded(false);
        if (articleRecords.length === 0) return;
        setArticleItem(articleRecords.filter(article => article.pmId === a)[0]);
        //console.log("Fetching PMID "+a);
        fetch(endpoint + "/variant/textAnalysis", {method: "POST", headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({pmId: a, variant: variantSelected, term: variantSelected.split(":")[1]}) })
            .then(result => result.json())
            .then(data => {
                setTextAnalysis(data);
                if (data.length > 0) {
                    if (data[0].islands == null) {
                        setTextView("Paragraph");
                        setInnerHTML(renderText("Paragraph", data[0], true));
                    } else setInnerHTML(renderText(textView, data[0], true));
                    setInnerHTML2(renderText2(data[0]));
                }
            });
    }

    const loadVariant = (v) => {
        updateVariant();
        if (v != null && v !== "") {
            let v1 = variants.filter(s => s.descriptor === v)[0];
            if (variant === null || variant.id !== v1.id) {
                setVariant(v1);
                if (v1.articlesTier1 != null && v1.articlesTier1.length > 0) {
                    let list = v1.articlesTier1;   // we need articlesTier1, no more consensus stuff
                    if (v1.excludedPMIds != null) {
                        let exclusionList = v1.excludedPMIds.split(", ");
                        let removed = [];
                        for (let i = 0; i < exclusionList.length; i++)
                            if (list.indexOf(exclusionList[i]) >= 0) {
                                removed = list.splice(list.indexOf(exclusionList[i]), 1);
                                if (removed.length > 0) removed.forEach(element => list.push("[excluded] " + element));
                            }
                    }
                    setArticleList(list);
                    let list1 = [...list];
                    if (v1.articlesTier2 != null && v1.articlesTier2.length > 0) {
                        list = v1.articlesTier2;
                        if (v1.excludedPMIds != null) {
                            let exclusionList = v1.excludedPMIds.split(", ");
                            let removed = [];
                            for (let i = 0; i < exclusionList.length; i++)
                                if (list.indexOf(exclusionList[i]) >= 0) {
                                    removed = list.splice(list.indexOf(exclusionList[i]), 1);
                                    if (removed.length > 0) removed.forEach(element => list.push("[excluded] " + element));
                                }
                        }
                        setArticleList2(list);
                    } else {
                        list = [];
                        setArticleList2([]);
                    }
                    if (v1.notes !== null) setNoteBox(v1.notes);
                    else setNoteBox("");
                    let data = list1.concat(list);
                    fetch(endpoint + "/fetch", {
                        method: "POST",
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(data)
                    }).then(result => result.json()).then((data) => {
                        setArticleRecords(data);
                        setTextPages(0);
                        //console.log(data);
                    });
                } else {
                    setArticleItem(null);
                    setTextAnalysis(null);
                    setTextPages(0);
                    setArticleList([]);
                    setArticleList2([]);
                }
                //console.log(v1.consensusPMIds.split(", "));
            }
        }
    }

    const submitJob = (single = true) => {
        //console.log("Submit...");
        if (single) {
            fetch(endpoint+"/runVariant?descriptor="+encodeURIComponent(variantSelected))
                .then(response => response.text()).then(data => {console.log(data); setJobModalShow(true);});
        } else {
            fetch(endpoint+"/runVariants/" + variantKeySelected)
                .then(response => response.text()).then(data => {setJobName(data); setJobModalShow(true);});
        }
    }

    const escape = (s) => {
        return s.replaceAll("*", "\\*");
    }

    const redirectGene = () => {
        let v = {variant: variant, variants: variants, variantKeys: variantKeys, variantSelected: variantSelected, geneName: geneName, cancerName: cancerName, drugs: drugs,
            variantKeySelected: variantKeySelected, articleItem: articleItem, articleRecords: articleRecords, article: article, articleList: articleList,
            topArticles: topArticles, textAnalysis: textAnalysis, textView: textView, innerHTML: innerHTML, processing: processing, uploadFiles: uploadFiles,
            createVariantKey: createVariantKey, submitRun: submitRun, noteBox: noteBox, excluded: excluded, textPages: textPages};
        fetch(endpoint+"/targets/search/findAllBySymbol?symbol="+encodeURI(geneName.toUpperCase())).then(response => response.json())
            .then(result => {
                if (result._embedded.targets.length > 0) {
                    props.loadGene(result._embedded.targets[0], v);
                }
            });
    }

    const renderText = (tView, tAnalysis = null, page1 = false) => {
        //console.log("rendering text");
        let pageNumbers;
        if (textAnalysis == null || textAnalysis[page1 ? 0 : textPages].pageNumbers != null) {
            pageNumbers = tAnalysis === null ? textAnalysis[page1 ? 0 : textPages].pageNumbers : tAnalysis.pageNumbers;
        }
        //console.log(pageNumbers);
        let analysis = tAnalysis === null ? textAnalysis[page1 ? 0 : textPages] : tAnalysis;
        let term = tAnalysis === null ? textAnalysis[page1 ? 0 : textPages].term : tAnalysis.term;
        let pmId = tAnalysis === null ? textAnalysis[page1 ? 0 : textPages].pmId : tAnalysis.pmId;
        let result = "<b>Source document is " + pmId + "</b><br/><br/>";
        if (tView === "Sentence Island" && analysis.islands != null) {
            let text = tAnalysis === null ? textAnalysis[page1 ? 0 : textPages].islands : tAnalysis.islands;
            //console.log(text);
            let island = 0;
            let count = 0;
            let pageList = [];
            for (let i = 0; i < text.length; i++) {
                let sentence = text[i][0];
                if (parseInt(text[i][1]) === island) {
                    if (sentence.indexOf(term) !== -1) {
                        let size = (sentence.match(new RegExp(escape(term), 'g'))).length;
                        for (let i = 0; i < size; i++)
                          pageList.push(pageNumbers[count++]);
                    }
                    result += sentence.replaceAll("-\n", "").replaceAll("- \n", "").replaceAll("\n", " ").replaceAll("  ", " ").replaceAll(term, "<b><mark>"+term+"</mark></b>") + "  ";
                } else {
                    if (pageList.length > 0) {
                        let pageStart = pageList[0];
                        let pageEnd = pageList[pageList.length - 1];
                        result += "<br/>";
                        if (pageEnd === pageStart) result += "<b>(page " + pageStart + ")</b>";
                        else result += "<b>(pages " + pageStart + "-" + pageEnd + ")</b>";
                    }
                    result += "<br/><br/>" + sentence.replaceAll("-\n", "").replaceAll("- \n", "").replaceAll("\n", " ").replaceAll("  ", " ").replaceAll(term, "<b><mark>"+term+"</mark></b>");
                    pageList = [];
                    if (sentence.indexOf(term) !== -1) {
                        let size = (sentence.match(new RegExp(escape(term), 'g'))).length;
                        for (let i = 0; i < size; i++)
                            pageList.push(pageNumbers[count++]);
                    }
                }
                island = parseInt(text[i][1]);
            }
            if (pageList.length > 0) {
                let pageStart = pageList[0];
                let pageEnd = pageList[pageList.length - 1];
                result += "<br/>";
                if (pageEnd === pageStart) result += "<b>(page " + pageStart + ")</b>";
                else result += "<b>(pages " + pageStart + "-" + pageEnd + ")</b>";
            }
            return result;
        } else if (tView === "Paragraph" || analysis.islands == null) {

            let index = 0;
            //console.log(tAnalysis === null ? textAnalysis.paragraphs : tAnalysis.paragraphs);
            let text = analysis.paragraphs;
            //console.log(text[0]);
            //console.log("Doing paragraph view");
            for (let i = 0; i < text.length; i++) {
                let size;
                if (analysis.islands == null) size = 1;
                else {
                    size = (text[i].match(new RegExp(escape(term), 'g'))).length;
                }
                result += text[i].replaceAll("-\n", "").replaceAll("- \n", "").replaceAll("\n", " ").replaceAll("  ", " ").replaceAll(term, "<b><mark>"+term+"</mark></b>");
                let pageStart = pageNumbers[index];
                let pageEnd = pageNumbers[index+size-1];
                result += "<br/>"
                if (analysis.islands == null && text.length === 1 && pageStart === pageEnd) {
                    result += "<b>(ABSTRACT ONLY)</b>";
                    break;
                }
                else if (pageStart === pageEnd) result += "<b>(Page " + pageStart + ")</b>";
                else result += "<b>(Pages " + pageStart + "-" + pageEnd + ")</b>";
                index += size;
                result += "<br/><br/>";
            }
            return result;
        } else {
            let text = tAnalysis === null ? textAnalysis[page1 ? 0 : textPages].islands : tAnalysis.islands;
            let count = 0;
            for (let i = 0; i < text.length; i++) {
                let sentence = text[i][0];
                if (sentence.indexOf(term) !== -1) {
                    let size = (sentence.match(new RegExp(escape(term), 'g'))).length;
                    result += sentence.replaceAll("\n", " ").replaceAll("-\n", "").replaceAll("- \n", "").replaceAll("  ", " ").replaceAll(term, "<b><mark>"+term+"</mark></b>");
                    result += "<br/><b>(page " + pageNumbers[count] + ")</b><br/><br/>";
                    for (let i = 0; i < size; i++) count++;
                }
            }
            return result;
        }
    }

    const renderText2 = (textAnalysis) => {
        let result = "";
        textAnalysis.keywordSentences.forEach((v, i) => {
            result += v.replaceAll("<mark>", "<b><mark>").replaceAll("</mark>", "</mark></b>");
            if (textAnalysis.keywordPageNumbers[i] != null && textAnalysis.keywordPageNumbers[i] > 0) {
                result += " <b>Page " + textAnalysis.keywordPageNumbers[i] + "</b><br/><br/>";
            } else {
                if (textAnalysis.keywordPageNumbers[i] != null && textAnalysis.keywordPageNumbers[i] === -1)
                    result += " <b>From Abstract/Pubmed Record</b><br/><br/>";
                if (textAnalysis.keywordPageNumbers[i] != null && textAnalysis.keywordPageNumbers[i] === 0)
                    result += " <b>From Full-text Header</b><br/><br/>";
            }
        });
        return result;
    }

    const sendData = (formData) => {
        //console.log("ADDING ITEM");
        //console.log(formData);

        fetch(endpoint + "/addVariants/"+createVariantKey, {method: "POST", body: formData}).then(response => response.text()).then(data => {
            //console.log("Added");
            //console.log(data);
            setProcessing(false);
            setCreateVariantKey("");
            setUploadFiles(null);
            setSubmitRun(false);
            reloadVariants();
        }).catch(error => alert(error));
    }

    const onFormSubmit = (e) => {
        setProcessing(true);
        const form = e.currentTarget;
        if (form.checkValidity() === false) {
            e.preventDefault();
            e.stopPropagation();
        }
        e.preventDefault();
        const formData = new FormData(e.target);
        //formData.append("attachment", uploadFiles[0], uploadFiles[0].name);
        sendData(formData);
        e.target.reset();
    }

    const strip = (s) => {
        if (s.indexOf("[excluded] ") >= 0) return s.substring("[excluded] ".length);
        return s;
    }

    const downloadSpreadsheet = () => {
        let k = variantKeySelected;
        if (variantKeySelected === "none") k = "_no_key";
        return endpoint + "/variants/download/autocuration" + k + ".xlsx";
    }

    const redirectArticle = () => {
        let v = {variant: variant, variants: variants, variantKeys: variantKeys, variantSelected: variantSelected, geneName: geneName, cancerName: cancerName, drugs: drugs,
            variantKeySelected: variantKeySelected, articleItem: articleItem, articleRecords: articleRecords, article: article, articleList: articleList,
            topArticles: topArticles, textAnalysis: textAnalysis, textView: textView, innerHTML: innerHTML, processing: processing, uploadFiles: uploadFiles,
            createVariantKey: createVariantKey, submitRun: submitRun, noteBox: noteBox, excluded: excluded};
        props.readArticle(articleItem, v);
    }

    const splitDrugs = (drugs) => {
        let result = "";
        drugs.forEach(d => result += ","+d);
        return result;
    }

    const oldScore = (pmid) => {
        let index = -1;
        if (variant.articlesTier1 != null && variant.articlesTier1.length > 0) variant.articlesTier1.forEach((x,i) => {if (pmid === x) index = i});
        if (index !== -1) return " (was alt:" + variant.scores1[index] + ", f:" + variant.keywordScores[index] + ", code:" + variant.scoreCode1[index] + ")";
        if (variant.articlesTier2 != null && variant.articlesTier2.length > 0) variant.articlesTier2.forEach((x,i) => {if (pmid === x) index = i});
        if (index !== -1) return " (was alt:" + variant.scores2[index] + ", f:" + variant.keywordScores[index+5] + ", code:" + variant.scoreCode2[index] + ")";
        return "";
    }


    return (
      <Container>
          <Row className="justify-content-md-center">
              <Card>
                  <Card.Header>Add new variants with a spreadsheet (.xlsx)</Card.Header>
                  <Card.Body>
                      <Card.Title>Choose a key and upload the file</Card.Title>
                      <Card.Text>
                          <Form ref={formRef} onSubmit={onFormSubmit}>
                              <Form.Group className="mb-3" controlId="FormText1">
                                  <Form.Label>Variant Key</Form.Label>
                                  <Form.Control size="lg" type="text" value={createVariantKey} onChange={(e) => setCreateVariantKey(e.target.value)} placeholder="(Mandatory) Enter key for accessing new variants" />
                              </Form.Group>
                              <Form.Group className="mb-3" controlId="FormFile1">
                                  <Form.Control type="file" className="custom-file-input" onChange={(e) => setUploadFiles(e.target.files)} label="Choose Excel .xlsx file" name="attachment" />
                              </Form.Group>
                              <Form.Group className="mb-3" controlId="FormCheckbox1">
                                  <Form.Check type="checkbox" label="Submit and run analysis" onChange={() => setSubmitRun(!submitRun)} checked={submitRun} name="submitrun" />
                              </Form.Group>
                              <Button type="submit" disabled={uploadFiles === null || createVariantKey === "" || processing}>Submit New Variants</Button></Form>
                      </Card.Text>
                  </Card.Body>
              </Card>
          </Row>
          <hr/>
          <Row className="justify-content-md-center">
              <Col xs lg="3"><h4 className="justify-content-md-center">Load Existing Records</h4></Col> {geneName !== '' && <Col><Button size="sm" onClick={() => redirectGene()}>{geneName}</Button></Col>}
          </Row>
          <Row className="justify-content-md-center">
              <Col><FloatingLabel controlId="floatingSelect2" label="Filter alterations by key"><Form.Select id="kselect" name="variantKeySelected" onChange={(e) => changeEvent(e)} value={variantKeySelected}>
                  <option value="none" key="-1">--Select a key below--</option>{getVariantKeys()}</Form.Select></FloatingLabel></Col>
              <Col><FloatingLabel controlId="floatingSelect1" label="Pick alteration"><Form.Select id="vselect" name="variantSelected" onChange={(e) => changeEvent(e)} value={variantSelected}>
                  <option value="" key="-1">--Select a variation below--</option>{getVariantItems()}</Form.Select></FloatingLabel></Col>
          </Row>
          <Row className="justify-content-md-center">
              <Col><Button variant="dark" href={downloadSpreadsheet()} target="_blank" rel="noreferrer">Download Spreadsheet for Key</Button>{' '}<Button variant="dark" onClick={() => submitJob(false)}>Run Curation analysis for Key</Button></Col>
              <Col>Or search for alteration by typing: <Select value={variantSelected} onChange={e => reactSelect(e)} options={getVariantOptions()}></Select></Col>
          </Row>
          {variantSelected !== "" ?
          <>
          <Row className="justify-content-md-center">
              <Col>Limit Possibly Relevant results display to
                  <Form.Select id="articleMax" name="topArticles" onChange={(e) => changeEvent(e)} value={topArticles}>
                      <option key="0" value={2}>2 articles</option>
                      <option key="1" value={5} defaultValue>5 articles</option>
                      <option key="2" value={10}>10 articles</option>
                      <option key="3" value={15}>15 articles</option>
                      <option key="4" value={30}>30 articles</option>
                      <option key="5" value={600}>All articles</option>
                  </Form.Select>
              </Col>
          </Row>
          <br/>
          <Row className="justify-content-md-center">
          <Col>
              {articleList.length > 0 ?
              <>
              <h3>{tiers[tier]}</h3>
              <ListGroup>
                  {variant != null && variant.newScores != null && variant.newScores.slice(0,5).map((a, i) => (<ListGroup.Item key={`${i}`} active={article === strip(a.pmId)} action onClick={() => loadArticle(strip(a.pmId))}>{a.pmId + " [score: " + a.total + " alt: " + a.altScore + ", f: " + a.fscore + (a.total == a.altScore+a.fscore+20 ? ", gene: +20]" : "]") + oldScore(a.pmId)}</ListGroup.Item>))}
              </ListGroup>
              </>: <Button onClick={() => submitJob()}>Process this variation</Button>}
          </Col>
          <Col>
              {/*             Selected Group:
              <DropdownButton id="dropdown-tier" onSelect={(e) => setTier(e)} title={tiers[tier]}>
                  <Dropdown.Item eventKey={0}>Most Relevant</Dropdown.Item>
                  <Dropdown.Item eventKey={1}>Possibly Relevant</Dropdown.Item>
                  <Dropdown.Item eventKey={2}>All Articles</Dropdown.Item>
              </DropdownButton> */}
          {variant.newScores != null && variant.newScores.length > 0 &&
              <><h3>Possibly Relevant</h3>
              <ListGroup>
                  {variant.newScores.slice(5, 5+topArticles).map((a, i) => (<ListGroup.Item key={`${i}`} active={article === strip(a.pmId)} action onClick={() => loadArticle(strip(a.pmId), true)}>{a.pmId + " [score: " + a.total + " (alt: " + a.altScore + ", f: " + a.fscore + (a.total == a.altScore+a.fscore+20 ? ", gene: +20]" : "]") + oldScore(a.pmId)}</ListGroup.Item>))}
              </ListGroup></>}
          </Col>
          </Row>
          <br/>
          {articleItem !== null && textAnalysis !== null && textAnalysis.length > 0 ?
          <Row className="justify-content-md-center">
              <Col>
                  <Card>
                      <Card.Header>View and Annotate Result</Card.Header>
                      <Card.Body>
                          <Card.Title>{article}: <b>{articleItem.title}</b> ({articleItem.citation})</Card.Title>
                          <Card.Text>
                              {textAnalysis[textPages] != null && <Button href={endpoint + "/getPDF/" + textAnalysis[textPages].pmId + ".pdf" + encodeURI("?terms=" + geneName + ",$ALTERATION-" + textAnalysis[textPages].term + ",$KEYWORDS" + (cancerName !== "" ? ","+cancerName : "")
                                  + (drugs.length > 0 ? splitDrugs(drugs) : ""))} variant="dark" size="lg" target="_blank" hidden={textAnalysis[textPages].islands == null} rel="noreferrer">View PDF</Button>}
                              &nbsp;&nbsp;<span className="text-large"><Button variant="dark" onClick={() => redirectArticle()}>Full Article Data</Button>
                              {textAnalysis.length > 1 &&
                              <DropdownButton id="dropdown-basic-button" title="Multiple Document Matches">
                                  {textAnalysis.map((t, i) => (<Dropdown.Item eventKey={i} onClick={() => updateTextAnalysis(i)}> {textAnalysis[i].pmId} </Dropdown.Item>))}
                              </DropdownButton>}</span>
                              <hr/>
                              <Form>
                                  <div className="mb-3">
                                  <Form.Group controlId="FormCheckbox2">
                                      <Form.Check type="checkbox" label={"Exclude this Article " + articleItem.pmId} name="excludedcheck" checked={excluded} onChange={() => setExcluded(!excluded)} />
                                  </Form.Group>
                                  <Form.Group controlId="exampleForm.ControlTextarea1">
                                      <Form.Label>Notes on Variant - e.g., reasons for deletion of PMIDs, logging best results, etc.</Form.Label>
                                      <Form.Control as="textarea" rows={3} name="notes" value={noteBox} onChange={(e) => setNoteBox(e.target.value)} />
                                  </Form.Group>
                                  </div>
                              </Form>
                          </Card.Text>
                      </Card.Body>
                  </Card>
              </Col>
              <Col>
                  <Tabs defaultActiveKey="alteration" activeKey={key} onSelect={(k) => setKey(k)} id="sentencetabs">
                  <Tab eventKey="alteration" title="Alteration">
                  <Card>
                      <Card.Header>Text Results from Article</Card.Header>
                      <Card.Body>
                          <Card.Title>Article PMID {article}: <b>{articleItem.title}</b> ({articleItem.citation})</Card.Title>
                          <Card.Text>
                              {textAnalysis[textPages] != null && <Form.Select id="textView" name="textView" onChange={(e) => changeEvent(e)} value={textView}>
                                  <option key="0" disabled={textAnalysis[textPages].islands == null} value="Sentence Island" defaultValue>Sentence Island</option>
                                  <option key="1" value="Paragraph">Paragraphs</option>
                                  {/*<option key="2" disabled={textAnalysis[textPages].islands == null} value="Sentences">Exact Sentences</option>*/}
                              </Form.Select>}
                              <br/>
                              <h4>From the text...</h4>
                              <div style={{fontFamily: "sans-serif"}} dangerouslySetInnerHTML={{__html: innerHTML}} />
                          </Card.Text>
                      </Card.Body>
                  </Card>
                  </Tab>
                  <Tab eventKey="keywords" title="Functional Keywords">
                      <Card>
                          <Card.Header>Functional Keywords from Article</Card.Header>
                          <Card.Body>
                              <Card.Title>Article PMID {article}: <b>{articleItem.title}</b> ({articleItem.citation})</Card.Title>
                              <Card.Text>
                                  <h4>From the text...</h4>
                                  <div style={{fontFamily: "sans-serif"}} dangerouslySetInnerHTML={{__html: innerHTML2}} />
                              </Card.Text>
                          </Card.Body>
                      </Card>
                  </Tab>
                  </Tabs>
              </Col>
          </Row>: null}
          </>
          : null}
          <JobModal show={jobModalShow} jobName={jobName} onHide={() => setJobModalShow(false)}/>
      </Container>
    );

}

export default Analyzer;