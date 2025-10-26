import {useState, useEffect} from 'react';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import InputGroup from 'react-bootstrap/InputGroup';
import FormControl from 'react-bootstrap/FormControl';
import {ProfileContent} from './components/ProfileViews.js';
import SearchBar from './components/SearchBar.js';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import Card from 'react-bootstrap/Card';
import Alert from 'react-bootstrap/Alert';
import Form from 'react-bootstrap/Form';
import Dropdown from 'react-bootstrap/Dropdown';
import Pagination from 'react-bootstrap/Pagination';
import PageItem from 'react-bootstrap/PageItem';
import DropdownButton from 'react-bootstrap/DropdownButton';
import CloseButton from 'react-bootstrap/CloseButton';
import Collapse from 'react-bootstrap/Collapse';
import Popover from 'react-bootstrap/Popover';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import Spinner from 'react-bootstrap/Spinner';
import Toast from 'react-bootstrap/Toast';

import TargetGene from './components/TargetGene/TargetGene.js';
import Article from './components/Article/Article.js';
import Analyzer from './components/Analyzer/Analyzer';
import Resources from './components/Resources/Resources';
import Filters from './components/Filters/Filters.js';
import LoginModal from './components/Modals/LoginModal';
import ErrorModal from './components/Modals/ErrorModal';
import SubmitModal from './components/Modals/SubmitModal';
import * as aa from './components/AminoAcids.js';

import './Intuition.scss';
import 'bootstrap/dist/css/bootstrap.min.css';
import endpoint from './endpoint.js';
import styles from './SearchBar.module.css';

// spoof prefix = https://cors-anywhere.herokuapp.com/

const Intuition = (props) => {

  const [searchText, setSearchText] = useState('');
  const [modalShow, setModalShow] = useState(false);
  const [user, setUser] = useState(null);
  const [userName, setUserName] = useState(null);
  const [reload, setReload] = useState(1);
  const [showLogin, setShowLogin] = useState(true);
  const [showMessage, setShowMessage] = useState(true);
  const [filterCall, setFilterCall] = useState(1);
  const [filterClear, setFilterClear] = useState(2);
  const [showFull, setShowFull] = useState(null);
  const [hasItem, setHasItem] = useState(false);
  const [item, setItem] = useState("");
  const [itemType, setItemType] = useState("");
  const [itemVariant, setItemVariant] = useState(null);
  const [targetData, setTargetData] = useState(null);

  const [isAuthenticated, setAuthenticated] = useState(false);
  const [mutation1, setMutation1] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [mutation2, setMutation2] = useState('');
  const [error, setError] = useState('');
  const [key, setKey] = useState('home');
  const [groupItems, setGroupItems] = useState([null, null, null, null, []]);
  const [filters, setFilters] = useState([[], []]);
  const [articles, setArticles] = useState([]);
  const [articleSpinner, setArticleSpinner] = useState(false);
  const [limit, setLimit] = useState("100");
  const [authors, setAuthors] = useState('');
  const [clearSearch, setClearSearch] = useState(1);
  const [found, setFound] = useState(true);
  const [status, setStatus] = useState(null);
  const [searchTerms, setSearchTerms] = useState('');
  const [pageNumber, setPageNumber] = useState(1);
  const [pagination, setPagination] = useState([]);
  const [downloadSubmit, setDownloadSubmit] = useState(false);

  const [date, setDate] = useState('');
  const [flashAnalyzer, setFlashAnalyzer] = useState(0);
  const [analyzerState, setAnalyzerState] = useState(null);
  const [selectedVariant, setSelectedVariant] = useState(null);

  const [timer, setTimer] = useState(null);

  useEffect(() => {
    if (props.state !== undefined && props.state.length > 12) {
      restoreState(props.state);
      //console.log("State restored");
    }
  }, [props.state]);

  useEffect(() => {
    let status = [false, false, false, false];
    fetch(endpoint + "/genemaps/search/findAllSymbols")
    .then(data => data.json())
    .then(result => {
      let testItems = [];
      for (let i = 0; i < result._embedded.genemaps.length; i++) testItems.push(result._embedded.genemaps[i].symbol);
      testItems.sort();
      groupItems[0] = testItems;
      status[0] = true;
      if (status[0] && status[1] && status[2] && status[3]) {
        setGroupItems(groupItems);
        checkAuthentication();
      }
    });
    fetch(endpoint + "/mutationmaps/search/findAllSymbols")
    .then(data => data.json())
    .then(result => {
      let testItems = [];
      for (let i = 0; i < result._embedded.mutationmaps.length; i++) testItems.push(result._embedded.mutationmaps[i].symbol);
      testItems.sort();
      status[1] = true;
      groupItems[1] = testItems;
      if (status[0] && status[1] && status[2] && status[3]) {
        setGroupItems(groupItems);
        checkAuthentication();
      }
    });
    fetch(endpoint + "/drugmaps/search/findAllDrugs")
    .then(data => data.json())
    .then(result => {
      let testItems = [];
      for (let i = 0; i < result._embedded.drugmaps.length; i++) testItems.push(result._embedded.drugmaps[i].drug);
      testItems.sort();
      status[2] = true;
      groupItems[2] = testItems;
      if (status[0] && status[1] && status[2] && status[3]) {
        setGroupItems(groupItems);
        checkAuthentication();
      }
    });
    fetch(endpoint + "/cancermaps/search/findAllCancerTypes")
    .then(data => data.json())
    .then(result => {
      let testItems = [];
      for (let i = 0; i < result._embedded.cancermaps.length; i++) testItems.push(result._embedded.cancermaps[i].cancerType);
      testItems.sort();
      status[3] = true;
      groupItems[3] = testItems;
      if (status[0] && status[1] && status[2] && status[3]) {
        setGroupItems(groupItems);
        checkAuthentication();
      }
    });
  }, []);

  useEffect(() => {
    if (props.match.params.targetId === undefined) return;
    let value = props.match.params.targetId;
    if (value === '' || value === null) return;
    fetch(endpoint+"/targets/search/findAllBySymbol?symbol="+encodeURI(value.toUpperCase())).then(response => response.json())
    .then(result => {
      if (result._embedded.targets.length > 0) {
        loadComplete(result._embedded.targets[0], false);
      }
    });
  }, [props.match.params.targetId]);

  useEffect(() => {
    if (props.match.params.pmId === undefined) return;
    let value = props.match.params.pmId;
    if (value === '' || value === null) return;
    fetch(endpoint+"/articles/search/findByPmId?pmId="+encodeURI(value)).then(response => response.json())
    .then(result => {
      if (result.pmId !== undefined) {
        loadCompleteArticle(result, false, itemVariant);
      }
    });
  }, [props.match.params.pmId]);

  const getState = (analyzerValue = null) => {
    return [suggestions, hasItem, targetData, searchText, key, item, itemType, showMessage, user, reload, userName, filters, articles, groupItems, hasItem, searchTerms, status, analyzerValue === null ? analyzerState : analyzerValue, selectedVariant, analyzerValue === null ? itemVariant : analyzerValue.variant];
  }

  const addAnalyzerState = (state) => {
    if (selectedVariant !== null) {  // change the state of the Analyzer with passed back variant
      state.variantKeySelected = "none";
      state.variantSelected = selectedVariant.gene+":"+(selectedVariant.mutation !== null ? selectedVariant.mutation: "")+":"+(selectedVariant.cancerTypes !== null ? selectedVariant.cancerTypes: "")+":"+(selectedVariant.drugs !== null ? selectedVariant.drugs: "");
      state.variant = selectedVariant;
      state.textAnalysis = null;
      state.geneName = selectedVariant.gene;
      if (selectedVariant.cancerTypes != null) state.cancerName = selectedVariant.cancerTypes;
      if (selectedVariant.drugs !== null) state.drugs = selectedVariant.drugs.split(", ");
      if (selectedVariant.consensusPMIds !== null) {
        let list = selectedVariant.consensusPMIds.split(", ");
        if (selectedVariant.excludedPMIds !== null) {
          let exclusionList = selectedVariant.excludedPMIds.split(", ");
          let removed = [];
          for (let i = 0; i < exclusionList.length; i++)
            if (list.indexOf(exclusionList[i]) >= 0) {
              removed = list.splice(list.indexOf(exclusionList[i]), 1);
              if (removed.length > 0) removed.forEach(element => list.push("[excluded] " + element));
            }
        }

        state.articleList = list;
        if (selectedVariant.notes !== null) state.noteBox = selectedVariant.notes;
        else state.noteBox = "";
      }
      fetch(endpoint + "/fetch", {
        method: "POST",
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(selectedVariant.consensusPMIds.split(", "))
      }).then(result => result.json()).then((data) => {
        state.articleRecords = data;
        setAnalyzerState(state);
      });
      setKey('analyze');
      setSelectedVariant(null);
    }
    else setAnalyzerState(state);
    return state;
  }

  const restoreState = (state) => {
    setSuggestions(state[0]);
    setHasItem(state[1]);
    setTargetData(state[2]);
    setSearchText(state[3]);
    setKey(state[4]);
    setItem(state[5]);
    setItemType(state[6]);
    setShowMessage(state[7]);
    setUser(state[8]);
    setReload(state[9]);
    setUserName(state[10]);
    setFilters(state[11]);
    setArticles(state[12]);
    setGroupItems(state[13]);
    setHasItem(state[14]);
    setSearchTerms(state[15]);
    setStatus(state[16]);
    setAnalyzerState(state[17]);
    setSelectedVariant(state[18]);
    setItemVariant(state[19]);
  }

  const checkAuthentication = () => {
    fetch(endpoint + "/conf/user")
    .then(response => response.text())
    .then(data => {
      if (data.endsWith("mskcc.org")) {
        setAuthenticated(true);
        setUserName(data);
      } else if (data.endsWith("google.com")) {
        setAuthenticated(true);
        setUserName(data);
      }
    });
    fetch(endpoint + "/conf/user/me")
    .then(response => response.json())
    .then(data => {
      if (data.emailAddress !== undefined) {
        setUser(data);
        setAuthenticated(true);
      }
      //if (reload) onReload();
    }).catch(e => {});
  }

  const loginSuccess = () => {
    setAuthenticated(true);
    //checkAuthentication();
    //setFlashGene(flashGene+1);
    //setFlashArticle(flashArticle+1);
    setFlashAnalyzer(flashAnalyzer => flashAnalyzer+1);
    fetch(endpoint + "/conf/user/me")
        .then(response => response.json())
        .then(data => {
          if (data.emailAddress !== undefined) setUser(data);
          //if (reload) onReload();
        }).catch(e => {});
  }

  const sendTimer = (t) => {
    setTimer(t);
  }

  const loadComplete = (data, onLoad = true) => {
    let itemtype = "target";
    //console.log(data);
    setItem(data);
    setHasItem(true);
    if (onLoad) props.onMove(key, getState(), "genes/" + data.symbol);
    setKey('item');
    setItemType(itemtype);
    //onReload();
  }

  const loadCompleteArticle = (data, onLoad = true, variant = null) => {
    let itemtype = "article";
    setItem(data);
    setHasItem(true);
    window.scroll({top:0,behavior:'smooth'});
    if (onLoad) props.onMove(key, getState(), "article/" + data.pmId);
    setKey("item");
    setItemType(itemtype);
    setItemVariant(variant);
  }

  const redirectCompleteArticle = (data, analyzerState) => {
    let itemtype = "article";
    //console.log(analyzerState.variant);
    setItem(data);
    setItemVariant(analyzerState.variant);
    setHasItem(true);
    window.scroll({top:0,behavior:'smooth'});
    props.onMove(key, getState(analyzerState), "article/" + data.pmId);
    setKey("item");
    setItemType(itemtype);
  }

  const redirectGene = (data, analyzerState) => {
    let itemtype = "target";
    setItem(data);
    setHasItem(true);
    window.scroll({top:0,behavior:'smooth'});
    props.onMove(key, getState(analyzerState), "genes/" + data.symbol);
    setKey('item');
    setItemType(itemtype);
  }

  const sendVariant = (v) => {
    setFlashAnalyzer(flashAnalyzer + 1);
    setSelectedVariant(v);
  }

  const handleLogin = () => {
    setShowLogin(true);
  }

  const handleLogout = () => {
    fetch(endpoint + "/logout")
    .then(() => {
      setAuthenticated(false);
      setUser(null);
      setUserName(null);
      if (timer !== null) {
        clearInterval(timer);
        setTimer(null);
      }
    });
  }

  const handleSelection = (value) => {
    if (value) {
      console.info(`Selected "${value}"`);
    }
  }


  const popover = (k) => {
    if (k === 'home')
    return (
        <Popover id="popover-basic">
          <Popover.Header as="h3">Welcome to the Gene Search widget!</Popover.Header>
          <Popover.Body>
            Suggestions - enter the symbol code (e.g. - TP53, KRAS) and then the point mutation (e.g. G12D) to expand
            search.
            Or, you can try searching any other term with your mutations, it does not have to be a gene-based search!
            There are two boxes
            provided here and one or both can be blank.
          </Popover.Body>
        </Popover>
    );
    if (k === 'curate')
    return (
        <Popover id="popover-basic2">
          <Popover.Header as="h3">Welcome to the article curation tool!</Popover.Header>
          <Popover.Body>
            Suggestions - the top filters can be set and "Add Filters" will add all valid ones to the item list, or you can try typing your terms
            into the search bar for suggestions and add them there.  Active filters appear as cards and "Clear Filters" will clear them.  Enter
            gene and mutation (and then possibly drug(s) and cancer type(s)) each as separate items from the four categories, and non-existant terms
            can be added as "custom" fields.
          </Popover.Body>
        </Popover>
    );
    if (k === 'analyze')
    return (
        <Popover id="popover-basic2">
          <Popover.Header as="h3">Welcome to the Variant Result Analyzer tool!</Popover.Header>
          <Popover.Body>
            Suggestions - you can select an item by first filtering by the key for the job, or you can pick individual variants directly from the database,
            to review the articles found for each alteration or alteration in a specific context (with drugs or only for specific cancer types).  For the
            returned articles, you can annotate these results to exclude or downrank them based on the content.
          </Popover.Body>
        </Popover>
    );
    if (k === 'resources')
      return (
          <Popover id="popover-basic2">
            <Popover.Header as="h3">Resource Usage Monitor</Popover.Header>
            <Popover.Body>
              The progress bar will give a sense of how saturated the system is at any given time.
            </Popover.Body>
          </Popover>
      )
  }

  const binarySearch = (x, val, n= 2, eq= false) => {
    let results = [];
    let start = 0;
    let end = x.length;
    let mid = Math.trunc(x.length/2);
    n = Math.min(x[mid].length, val.length);
    if (!eq) {
      while (x[mid].substring(0,n) !== val.substring(0,n) && end-start > 1) {
        if (val.substring(0,n) < x[mid].substring(0,n)) end = mid;
        if (val.substring(0,n) > x[mid].substring(0,n)) start = mid;
        mid = Math.trunc((start + end)/2);
        n = Math.min(x[mid].length, val.length);
      }
    } else {
      while (x[mid] !== val && end-start > 1) {
        if (val < x[mid]) end = mid;
        if (val > x[mid]) start = mid;
        mid = Math.trunc((start + end)/2);
        n = Math.min(x[mid].length, val.length);
      }
    }

    if (!eq) {
      if (x[mid].substring(0,n) === val.substring(0,n)) results.push(x[mid]);
    } else {
      if (x[mid] === val) results.push(x[mid]);
    }
    let mid_o = mid;
    if (!eq) {
      while (mid > 0 && x[mid-1].substring(0,n) === val.substring(0,n)) {
        mid--;
        results.push(x[mid]);
      }
    } else {
      while (mid > 0 && x[mid-1] === val) {
        mid--;
        results.push(x[mid]);
      }
    }
    mid = mid_o;
    if (!eq) {
      while (mid < x.length-1 && x[mid+1].substring(0,n) === val.substring(0,n)) {
        mid++;
        results.push(x[mid]);
      }
    } else {
      while (mid < x.length-1 && x[mid+1] === val) {
        mid++;
        results.push(x[mid]);
      }
    }
    return {values: results, mid: mid_o};
  }

  const textChanged = (event) => {
    //console.log(event.target.value);
    if (event.target.name === 'searchText')
      setSearchText(event.target.value);
    else if (event.target.name === 'mutation1')
      setMutation1(event.target.value);
    else if (event.target.name === 'mutation2')
      setMutation2(event.target.value);
  }

  const onReload = () => {
    setReload(reload+1);
  }

  const pubmedSearch = (carrot2 = false) => {
    let result = searchText;
    if (mutation1 !== "") {
      let res = mutation1.toUpperCase().match(aa.mutation());
      //console.log(res);
      if (res.length > 0 && res[0].length === mutation1.length) {
        result += " AND (" + mutation1.toUpperCase() + " OR " + aa.toLong(res[0]) + ")";
      } else {
        setModalShow(true);
        setError(mutation1);
        return;
      }
    }
    if (mutation2 !== "") {
      let res = mutation2.toUpperCase().match(aa.mutation());
      if (res.length > 0 && res[0].length === mutation2.length) {
        result += " AND (" + mutation2.toUpperCase() + " OR " + aa.toLong(res[0]) + ")";
      } else {
        setModalShow(true);
        setError(mutation2);
        return;
      }
    }
    let pURL = "";
    if (!carrot2) pURL = "https://aimlcoe.mskcc.org/frontend/#/search/pubmed/"+encodeURI(result)+"/treemap";
    else pURL = "https://aimlcoe.mskcc.org/carrot2solr/search?source=solr&view=folders&skin=fancy-compact&query="+encodeURI(result)+"&results=300&algorithm=stc&SolrDocumentSource.solrFilterQuery=&SolrDocumentSource.solrTitleFieldName=attr_pdf_docinfo_title&SolrDocumentSource.solrSummaryFieldName=attr_content&SolrDocumentSource.solrUrlFieldName=attr_fileurl&SolrDocumentSource.solrIdFieldName=id&SolrDocumentSource.readClusters=true&SolrDocumentSource.useHighlighterOutput=true";
    window.open(pURL, '_blank');
  }

  const handleSearch = (value) => {
    if (value) {
      //setSearching(true);
      setSuggestions([]);
      //console.info(`Searching "${value}"`);
      if (value.indexOf(":") !== -1) {
        let term = value.toLowerCase().substring(0, value.indexOf(":"));
        let type = -1;
        let index = -1;
        if (value.endsWith("in genes")) {
          type = 0;
          index = binarySearch(groupItems[0], term, 2, true).mid;
        }
        if (value.endsWith("in mutations")) {
          type = 1;
          index = binarySearch(groupItems[1], term, 2, true).mid;
        }
        if (value.endsWith("in drugs")) {
          type = 2;
          index = binarySearch(groupItems[2], term, 2, true).mid;
        }
        if (value.endsWith("in cancer types")) {
          type = 3;
          index = binarySearch(groupItems[3], term, 2, true).mid;
        }
        if (value.endsWith(": custom")) {
          type = 4;
          let v = groupItems;
          if (!v[4].includes(value.substring(0, value.indexOf(":")))) {
            v[4].push(value.substring(0, value.indexOf(":")));
            setGroupItems(v);
            index = v[4].length-1;
          } else return;
        }

        if (type === -1 || index === -1) return;
        let oldGroups = filters[0];
        let oldItems = filters[1];
        for (let j = 0; j < filters[0].length; j++) {
          if (filters[0][j] === type && filters[1][j] === index) return;
        }
        oldGroups.push(type);
        oldItems.push(index);
        setFilters([oldGroups, oldItems]);
        setSuggestions([]);
        setClearSearch(clearSearch+1);
      } else {
        let term = value.toLowerCase();
        let values = [];
        let type = -1;
        let index = -1;
        let result = binarySearch(groupItems[0], term, 2, true);
        //console.log(result);
        if (result.values.length > 0) {
          type = 0;
          index = result.mid;
          let oldGroups = filters[0];
          let oldItems = filters[1];
          for (let j = 0; j < filters[0].length; j++) {
            if (filters[0][j] === type && filters[1][j] === index) return;
          }
          oldGroups.push(type);
          oldItems.push(index);
          setFilters([oldGroups, oldItems]);
          setSuggestions([]);
          setClearSearch(clearSearch+1);
          return;
        }
        result = binarySearch(groupItems[1], term, 2, true);
        //console.log(result);
        if (result.values.length > 0) {
          type = 1;
          index = result.mid;
          let oldGroups = filters[0];
          let oldItems = filters[1];
          for (let j = 0; j < filters[0].length; j++) {
            if (filters[0][j] === type && filters[1][j] === index) return;
          }
          oldGroups.push(type);
          oldItems.push(index);
          setFilters([oldGroups, oldItems]);
          setSuggestions([]);
          setClearSearch(clearSearch+1);
          return;
        }
        result = binarySearch(groupItems[2], term, 2, true);
        //console.log(result);
        if (result.values.length > 0) {
          type = 2;
          index = result.mid;
          let oldGroups = filters[0];
          let oldItems = filters[1];
          for (let j = 0; j < filters[0].length; j++) {
            if (filters[0][j] === type && filters[1][j] === index) return;
          }
          oldGroups.push(type);
          oldItems.push(index);
          setFilters([oldGroups, oldItems]);
          setSuggestions([]);
          setClearSearch(clearSearch+1);
          return;
        }
        result = binarySearch(groupItems[3], term, 2, true);
        //console.log(result);
        if (result.values.length > 0) {
          type = 3;
          index = result.mid;
          let oldGroups = filters[0];
          let oldItems = filters[1];
          for (let j = 0; j < filters[0].length; j++) {
            if (filters[0][j] === type && filters[1][j] === index) return;
          }
          oldGroups.push(type);
          oldItems.push(index);
          setFilters([oldGroups, oldItems]);
          setSuggestions([]);
          setClearSearch(clearSearch+1);
          return;
        }
        type = 4;
        let v = groupItems;
        if (!v[4].includes(value)) {
          v[4].push(value);
          index = v[4].length-1;
          setGroupItems(v);
          let oldGroups = filters[0];
          let oldItems = filters[1];
          oldGroups.push(type);
          oldItems.push(index);
          setFilters([oldGroups, oldItems]);
          setSuggestions([]);
          setClearSearch(clearSearch+1);
        }
      }
    }
  }

  const handleClear = () => {
    setSuggestions([]);
  }

  const changeEvent = (e) => {
    if (e.target.name === "limit") setLimit(e.target.value);
  }

  const handleChange = (input) => {
    let og = input.toLowerCase();
    if (og.length < 3) return;
    //console.log(og + " " + groupItems[0][0]);
    let values1 = binarySearch(groupItems[0], og).values;
    let values2 = binarySearch(groupItems[1], og).values;
    let values3 = binarySearch(groupItems[2], og).values;
    let values4 = binarySearch(groupItems[3], og).values;
    let items = [];
    let exact = false;
    if (values1.length > 0)
      values1.forEach((x) => { items.push(x + ': in genes'); if (og === x) exact = true; });
    if (values2.length > 0)
      values2.forEach((x) => { items.push(x + ': in mutations'); if (og === x) exact = true; });
    if (values3.length > 0)
      values3.forEach((x) => { items.push(x + ': in drugs'); if (og === x) exact = true; });
    if (values4.length > 0)
      values4.forEach((x) => { items.push(x + ': in cancer types'); ; if (og === x) exact = true; });
    if (!exact) items.push(og + ": custom");
    setSuggestions(items);
    //if (input.indexOf(" ") === -1) input = "\"" + input + "\"";
  }

  const renderSearchHeader = () => {
    if (key === "home" || key === 'item') {
      return (
          <div className="searchBody"></div>
      /*<br/><br/>
      <b style={{color: "white"}}>Enter any search term that may or may not include gene of interest:</b>
      <InputGroup id="inputGroup-sizing-sm" className="mb-3">
        <FormControl name="searchText" onChange={textChanged} value={searchText} onKeyPress={event => {
            if (event.key === "Enter") {
              pubmedSearch(true);
            }
          }} placeholder="Enter any relevant search term, which can include quotation marks, AND, OR, NOT, parentheses etc." aria-label="Pubmed Search Term" aria-describedby="basic-addon2" />
      </InputGroup>
      <Row>
      <Col>
      <b style={{color: "white"}}>Mutation 1:</b>
      <InputGroup id="inputGroup-sizing-sm" className="mb-3">
        <FormControl name="mutation1" onChange={textChanged} value={mutation1} onKeyPress={event => {
            if (event.key === "Enter") {
              pubmedSearch();
            }
          }} placeholder="e.g. Y55R" aria-label="Pubmed Search Term" aria-describedby="basic-addon2" />
      </InputGroup>
      </Col>
      <Col>
      <b style={{color: "white"}}>Mutation 2:</b>
      <InputGroup id="inputGroup-sizing-sm" className="mb-3">
        <FormControl name="mutation2" onChange={textChanged} value={mutation2} onKeyPress={event => {
            if (event.key === "Enter") {
              pubmedSearch();
            }
          }} placeholder="e.g. A58G" aria-label="Pubmed Search Term" aria-describedby="basic-addon2" />
      </InputGroup>
      </Col>
      </Row>
      <Row>
      <Col style={{align: 'center'}}>
      <Button variant="secondary" onClick={() => pubmedSearch()}>Search and Cluster Pubmed</Button> <b style={{color: "white"}}>or</b> <Button variant="secondary" onClick={() => pubmedSearch(true)}>Clustering Search on Carrot2 full-text plus PDFs</Button>
      </Col>
      </Row>
      <br/>
      </div>*/
      );
    }
    if (key === "curate") {
      return (
        <div className="searchBody">
        <br/>
        <Filters groupItems={groupItems} count={filterCall} clear={filterClear} onFilterCall={runFilterCall}/>
        <br/><br/>
        <SearchBar autoFocus count={clearSearch} shouldRenderClearButton={true} shouldRenderSearchButton={true} placeholder="filters by gene/mutation/drug/cancer type name (one-at-a-time), or add arbitrary 'custom' search terms" suggestions={suggestions} onClear={handleClear} onChange={handleChange} onSelection={handleSelection} onSearch={handleSearch} styles={styles} />
        </div>
      );
    }
    if (key === 'analyze' || key === 'resources') {
      return (
        <div className="searchBody">

        </div>
      );
    }
  }

  const runFilterCall = (x) => {
    let oldGroups = filters[0];
    let oldItems = filters[1];
    for (let i = 0; i < 5; i++) {
      if (x[0][i] !== 0) {
        let found = false;
        for (let j = 0; j < i; j++) {
          if (filters[0][i] === filters[0][j] && filters[1][i] === filters[1][j]) found = true;
        }
        for (let j = 0; j < filters[0].length; j++) {
          if (filters[0][j] === (x[0][i]-1) && filters[1][j] === x[1][i]) found = true;
        }
        if (!found) {
          oldGroups.push(x[0][i]-1);
          oldItems.push(x[1][i]);
        }
      }
    }
    setFilters([oldGroups, oldItems]);
  }

/*
  const suggestionRenderer = (suggestion, searchTerm) => {
    return (
      <span>
        <span>{searchTerm}</span>
        <strong>{suggestion.substr(searchTerm.length)}</strong>
      </span>
    );
  }
*/
  const popoverTitle = () => {
    if (key === 'home' || key === 'item') return "Gene Search help";
    if (key === 'curate') return "Article Search help";
    if (key === 'analyze') return "Variant Analysis Help";
    if (key === 'resources') return "Resource Usage";
  }

  const filterName = (x) => {
    if (x === 0) return "Gene";
    if (x === 1) return "Mutation";
    if (x === 2) return "Drug";
    if (x === 3) return "Cancer Type";
    if (x === 4) return "Custom";
  }

  const removeFilter = (x) => {
    let newGroups = [];
    let newTypes = [];
    for (let i = 0; i < filters[0].length; i++) {
      if (i !== x) {
        newGroups.push(filters[0][i]);
        newTypes.push(filters[1][i]);
      }
    }
    setFilters([newGroups, newTypes]);
  }

  const runQuery = () => {
    setArticles([]);
    setArticleSpinner(true);
    let values = [];
    let terms = "";
    //filters[0].forEach((x,i) => values.push(groupItems[parseInt(x)][parseInt(filters[1][i])]));
    for (let i = 0; i < filters[0].length; i++) {
      values.push(groupItems[parseInt(filters[0][i])][parseInt(filters[1][i])]);
      if (filters[0][i] === 4) {
        if (terms === "") terms += groupItems[parseInt(filters[0][i])][parseInt(filters[1][i])];
        else terms += ";" + groupItems[parseInt(filters[0][i])][parseInt(filters[1][i])];
      }
    }
    setSearchTerms(terms);
    let data = {
      groups: filters[0],
      values: values,
      limit: parseInt(limit),
      authors: authors,
      searchTerms: terms,
      date: date
    };
    fetch(endpoint + "/query2", {method: "POST", headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data)})
    .then(response => response.json())
    .then(result => {
      //console.log(result);
      let ashow = [];
      if (result.length === 0) {
        setArticleSpinner(false);
        setFound(false);
        setError('');
      }
      else if (result.error != null) {
        setArticleSpinner(false);
        setFound(false);
        setError(result.error);
      }
      else {
        setFound(true);
        setError('');
        result.forEach(() => ashow.push(false));
        setShowFull(ashow);
        setArticles(result);
        let number = result.length / 100;
        if (number > 1) {
          let items = [];
          for (let n = 1; n <= number; n++) {
            items.push(n);
          }
          setPagination(items);
        }
        setArticleSpinner(false);
      }
    }).catch((error) => {
      console.error(error);
      setArticleSpinner(false);
      setFound(false);
    });
  }

  const addFilter = (value) => {
    //console.log(value);
    let type = -1;
    let index = -1;
    if (value.substring(0, value.indexOf(":")) === 'gene') {
      type = 0;
      index = binarySearch(groupItems[0], value.substring(value.indexOf(":")+1), 2, true).mid;
    }
    if (value.substring(0, value.indexOf(":")) === 'mutation') {
      type = 1;
      index = binarySearch(groupItems[1], value.substring(value.indexOf(":")+1), 2, true).mid;
    }
    if (value.substring(0, value.indexOf(":")) === 'drug') {
      type = 2;
      index = binarySearch(groupItems[2], value.substring(value.indexOf(":")+1), 2, true).mid;
    }
    if (value.substring(0, value.indexOf(":")) === 'cancer') {
      type = 3;
      index = binarySearch(groupItems[3], value.substring(value.indexOf(":")+1), 2, true).mid;
    }
    if (type === -1 || index === -1) return;
    let oldGroups = filters[0];
    let oldItems = filters[1];
    for (let j = 0; j < filters[0].length; j++) {
      if (filters[0][j] === type && filters[1][j] === index) return;
    }
    oldGroups.push(type);
    oldItems.push(index);
    setFilters([oldGroups, oldItems]);
  }

  const goDownload = () => {
    if (status !== null && status.includes("running")) {
      fetch(endpoint + "/checkStatus/" + status).then(result => result.text()).then(data => {
        if (data.includes(status) && !window.confirm("Still processing previous batches of downloads, start anyway?")) console.log("Skipping download job");
        else {
          let pmids = articles.map((a) => a.pmId);
          fetch(endpoint + "/downloadKnowledge", {
            method: "POST",
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(pmids)
          }).then(result => result.text()).then(data => {
            setDownloadSubmit(true);
            if (data.includes("running")) setStatus(data);
          });
        }
      });
    }
    else {
      let pmids = articles.map((a) => a.pmId);
      fetch(endpoint + "/downloadKnowledge", {
        method: "POST",
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(pmids)
      }).then(result => result.text()).then(data => {
        setDownloadSubmit(true);
        if (data.includes("running")) setStatus(data);
      });
    }
  }

  const linkify = (doi) => (<a href={"https://doi.org/" + doi.substring(0, doi.indexOf("/")) + doi.substring(doi.lastIndexOf("/"))} target="_blank" rel="noreferrer">{doi}</a>);
  const modifyShow = (i) => { let value = []; showFull.forEach(x => value.push(x)); value[i] = !value[i]; setShowFull(value); }

  return (
    <>
    <div className="App">
    <header className="bg-muted">
    <OverlayTrigger trigger="click" placement="right" overlay={popover(key)}>
      <Button variant="success">{popoverTitle()}</Button>
    </OverlayTrigger>
    <div style={{float: "right"}}>
    <DropdownButton id="dropdown-item-button" title={user !== null ? '' + user.emailAddress + '' : 'Logged Out'}>
      <Dropdown.ItemText><b>User Menu</b></Dropdown.ItemText>
      {isAuthenticated && user !== null ?
      <>
      <Dropdown.Item as="button" onClick={() => handleLogout()}>Logout{user !== null ? ' ' + user.emailAddress : ''}</Dropdown.Item>
      <ProfileContent user={user} />
      </> : <>
      <Dropdown.Item as="button" onClick={() => handleLogin()}>Login</Dropdown.Item>
      </>}
    </DropdownButton>
    {!isAuthenticated ? <LoginModal show={showLogin} onClose={() => setShowLogin(false)} auth={checkAuthentication} onAuthenticate={loginSuccess} /> : null}
    </div>
    </header>
    </div>
    <div className="searchHeader">
    {renderSearchHeader()}
    </div>
    <Tabs defaultActiveKey="home" activeKey={key} onSelect={(k) => setKey(k)} id="menutabs">
      <Tab eventKey="home" title="Home">
        <Card>
          <Card.Header>Description, Shortcuts, Instructions                                                                                                                                                                                                          </Card.Header>
          <Card.Body>
          <Card.Title>Article Search Tools</Card.Title>
          <Card.Text>
            Go to the "Search" page tab to filter by what genes, mutations, drugs, or cancer types you're looking for and get PDFs and supporting information!  Good search results?
            Hit the button "Download All Possible Results" and it will try to pull the PDF files.  In general, about 64% of articles can be obtained this way.
          </Card.Text>
          <Button variant="primary" onClick={() => setKey("curate")}>Go to Search</Button>
          </Card.Body>
        </Card>
        <Card>
          <Card.Body>
            <Card.Title>Analysis of Alterations and Suggested Articles</Card.Title>
            <Card.Text>
              First logging in, go to the "Analyze" page tab to pick a key for specific runs, or just select any alteration to bring up the relevant data (when it exists) or to start
              up a new alteration search.  Often, collecting the PDF articles first will greatly assist in finding specific locations in the text where the terms appear and you can
              look at three representations of text or directly open the highlighted PDF file.
            </Card.Text>
            <Button variant="primary" onClick={() => setKey("analyze")}>Go to Analyze</Button>
          </Card.Body>
        </Card>
        <div aria-live="polite" aria-atomic="true" style={{position: 'relative', minHeight: '100px'}}>
        <Toast style={{position: 'absolute', top: 0, left: 0}} show={showMessage} onClose={() => setShowMessage(false)}>
        <Toast.Header>
          <strong className="mr-auto">Curation Tools</strong>
        </Toast.Header>
        <Toast.Body>This site does not provide medical advice, please consult your practicing physician for all health-related questions.</Toast.Body>
        </Toast>
        </div>
      </Tab>
      <Tab eventKey="curate" title="Search">
      <Container className="border border-info">
        <Row className="justify-content-md-center">
          {filters[0].length > 0 ? filters[0].map((f, i) => (
            <Col xs lg="2">
            <Card>
              <Card.Header>Filter <CloseButton variant="dark" style={{float: "right"}} onClick={() => removeFilter(i)}/></Card.Header>
              <Card.Body>
              <Card.Title>{filterName(f)}</Card.Title>
              <Card.Text>
                {filterName(f) + ": " + groupItems[f][filters[1][i]]}
              </Card.Text>
              </Card.Body>
            </Card>
            </Col>
          )) : <Alert variant="info" style={{zIndex: '-1'}}>No filters added yet, use the Add Filters button with Filters #1-#4 at the top, or the Searchbar below them</Alert>}
        </Row>
        <Row>
        <br/>
        </Row>
        <Row className="justify-content-md-center">
          <Col xs lg="2" className="justify-content-md-center">
            <Button variant="primary" id="addFilters" onClick={() => setFilterCall(filterCall+1)}>Add Filters</Button>
          </Col>
          <Col xs lg="2" className="justify-content-md-center">
            <Button variant="primary" id="addFilters" onClick={() => {setFilterClear(filterClear+1); setFilters([[],[]]);}}>Clear Filters</Button>
          </Col>
        </Row>
        <Row>
          <br/>
        </Row>
        <Row className="justify-content-md-center">
          <Col xs lg="2">
          Consider articles after date <Form.Control className="justify-content-md-center" type="date" placeholder="Enter date" value={date} onChange={(e) => setDate(e.target.value)} name="date" />
          </Col>
          <Col xs lg="2">
          Limit search to
          <Form.Select id="articleMax" name="limit" onChange={(e) => changeEvent(e)} value={limit}>
            <option key="0" value="20">20 articles</option>
            <option key="1" value="100">100 articles</option>
            <option key="2" value="300">300 articles</option>
            <option key="3" value="500">500 articles</option>
            <option key="4" value="1000">1000 articles</option>
            <option key="5" value="2000">2000 articles</option>
            <option key="6" value="5000">5000 articles</option>
            <option key="7" value="20000">Maximum (20000 articles)</option>
          </Form.Select>
          </Col>
        </Row>
        <br/>
        <Row className="justify-content-md-center">
          <Col lg="4">
            <InputGroup className="mb-3">
              <InputGroup.Text>Authors</InputGroup.Text>
              <FormControl onChange={(e) => setAuthors(e.target.value)} value={authors} aria-label="Authors" aria-describedby="basic-addon1" placeholder="(optional) name of author(s)" />
            </InputGroup>
          </Col>
        </Row>
        <br/><br/>
        <Row className="justify-content-md-center">
          <Col xs lg="2" className="justify-content-md-center">
          <Button variant="outline-dark" onClick={() => runQuery()} disabled={filters[0].length === 0}>Query for Articles</Button>
          </Col>
        </Row>
        <br/><br/>
      </Container>
      {articles.length > 0 ?
      <Container className="border border-info">
      <Alert variant="primary">Displaying {articles.length} articles (max {limit}) <Button onClick={() => goDownload()} className="pull-right">Download All Possible Results</Button></Alert>
      {pagination.length > 0 && <Pagination className="justify-content-md-center">{pagination.slice(0, Math.min(pagination.length, 25)).map(n => <PageItem key={n} value={n} active={n === pageNumber} onClick={() => setPageNumber(n)}>{n}</PageItem>)}</Pagination>}
      {pagination.length > 25 && <Pagination className="justify-content-md-center">{pagination.slice(25, Math.min(pagination.length, 50)).map(n => <PageItem key={n} value={n} active={n === pageNumber} onClick={() => setPageNumber(n)}>{n}</PageItem>)}</Pagination>}
      {articles.slice((pageNumber-1)*100, pageNumber*100).map((a, i) => (
        <>
        <Card>
          <Card.Header>{a.publicationDate != null ? a.publicationDate.substring(0,10) : null} {a.citation} {a.inSupporting && <b>{"*** Found in supporting document(s) " + a.inSupporting + " ***"}</b>}</Card.Header>
          <Card.Body>
          <Card.Title><h2>{a.pmcId !== null && a.fulltext === null ? <Button href={endpoint + "/getPDF/" + a.pmId + ".pdf"} size="lg" target="_blank" rel="noreferrer">Download me!</Button> : a.fulltext !== null ? <><Button href={endpoint + "/getPDF/" + a.pmId + ".pdf" + (searchTerms.length > 0 ? encodeURI("?terms="+searchTerms) : "")} variant="dark" size="lg" target="_blank" rel="noreferrer">View PDF</Button> or <Button variant="dark" onClick={() => loadCompleteArticle(a)} size="lg">Full record/Supplementary</Button></>: <Button variant="dark" onClick={() => loadCompleteArticle(a)} size="lg">Full record</Button>}  {a.title}</h2></Card.Title>
          <Card.Text>
            <Row>
              <Col><b>AUTHORS:</b> {a.authors != null && a.authors.length > 400 ? a.authors.substring(0,400) + "..." : a.authors != null ? a.authors : null}</Col>
            </Row>
            <Row>
              <Col>
                <b>PubMed Id:</b> {a.pmId}
              </Col>
              <Col>
                <b>PMC Id:</b> {a.pmcId !== null ? a.pmcId : "NONE"}
              </Col>
              <Col>
                <b>doi:</b> {a.doi !== null ? linkify(a.doi) : "NONE"}
              </Col>
            </Row>
            <hr/>
            <Row>
              <Col>
                <b><i>{a.pubAbstract !== null ? <div dangerouslySetInnerHTML={{ __html: a.pubAbstract.replaceAll("-\n", "").replaceAll("\n", " ") }}/>: null}</i></b>
              </Col>
            </Row>
            <hr/>
            <Row className="justify-content-md-center">
              {a.keywords != null && a.keywords.length > 1 ? a.keywords.split(";").map((a,i) => (
                <Col xs lg="2">
                {/*<Card key={`${i}`}><Card.Header>Keyword</Card.Header>
                  <Card.Body>
                    <Card.Title>{a.substring(a.indexOf(":")+1)}</Card.Title>
                  </Card.Body>
                </Card>*/}
                Keyword: <b>{a.substring(a.indexOf(":")+1)}</b>
                </Col>)) : null}
              {a.meshTerms != null && a.meshTerms.length > 1 ? a.meshTerms.split(";").map((a,i) => (
                <Col xs lg="2">
                MeSH term: <b>{a.substring(a.lastIndexOf(":")+1)}</b>
                {/*<Card key={`${i}`}><Card.Header>MeSH term</Card.Header>
                  <Card.Body>
                    <Card.Title>{a.substring(a.lastIndexOf(":")+1)}</Card.Title>
                  </Card.Body>
                </Card>*/}
                </Col>)) : null}
                <Row className="justify-content-md-center">
                {a.topics != null && a.topics.length > 1 ? a.topics.split(";").map((av,i) => (
                  <Col xs lg="2">
                  <Card key={`${i}`}><Card.Header>Filter: {av.substring(0, av.indexOf(":"))} <Button onClick={() => addFilter(av)}>+</Button></Card.Header>
                    <Card.Body>
                      <Card.Title>{av.substring(av.lastIndexOf(":")+1)}</Card.Title>
                    </Card.Body>
                  </Card>
                  </Col>)) : null}
                </Row>
            </Row>
                {a.fulltext !== null ?
                  <>
                  <Row className="justify-content-md-center">
                  <Col xs lg="2">
                  <Button name={`${i}`} onClick={() => modifyShow(parseInt(i))}>
                  {showFull[parseInt(i)] ? "Hide Full Text" : "Show Full Text"}</Button>
                  </Col>
                  </Row>
                  <Row>
                    <Collapse in={showFull[i]}><i>{a.fulltext}</i></Collapse>
                  </Row>
                  </>
                : null}
          </Card.Text>
          </Card.Body>
        </Card>
        <br/><br/>
        </>
      ))}
      </Container>
      : articleSpinner ? <Row className="justify-content-md-center"><Col xs lg="1"><Spinner animation="border" variant="success" role="status"/></Col></Row>
      : !found ? error === "Forbidden" ? <Alert variant="secondary">Access Forbidden: Check that you are signed into the system</Alert>:<Alert variant="secondary">No articles were found, try other search criteria or search terms</Alert>: null}
      </Tab>
      <Tab eventKey="item" title="Items" disabled={!hasItem} tabClassName={hasItem ? '': 'd-none'}>
      {itemType === 'target' ? <TargetGene auth={isAuthenticated} item={item} page="item" push={sendVariant}/>
       : itemType === 'article' ? <Article auth={isAuthenticated} item={item} variant={itemVariant} searchTerms={searchTerms} createLink={linkify} />: null}
      </Tab>
      {isAuthenticated ?
      <Tab eventKey="analyze" title="Analyze">
        <Analyzer push={addAnalyzerState} flash={flashAnalyzer} state={analyzerState} readArticle={redirectCompleteArticle} loadGene={redirectGene}></Analyzer>
      </Tab> : null}
      {isAuthenticated ?
        <Tab eventKey="resources" title="Resources">
          <Resources push={sendTimer}></Resources>
        </Tab> : null}
    </Tabs>
    <ErrorModal show={modalShow} error={error} onHide={() => setModalShow(false)}/>
    <SubmitModal show={downloadSubmit} job={status} error={error} onClose={() => setDownloadSubmit(false)}/>
    </>
  );

};

export default Intuition;
