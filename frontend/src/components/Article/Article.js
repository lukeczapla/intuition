import React, {useEffect, useState, createRef} from 'react';
import endpoint from '../../endpoint.js';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import bsCustomFileInput from 'bs-custom-file-input';
import DropdownButton from "react-bootstrap/DropdownButton";
import Dropdown from "react-bootstrap/Dropdown";

const Article = (props) => {

  const [article, setArticle] = useState(props.item);
  const [variant, setVariant] = useState(props.variant);
  const [resourceData, setResourceData] = useState([]);
  const [processing, setProcessing] = useState(false);
  const [PMIDSupporting, setPMIDSupporting] = useState([]);
  const [PMIDSelected, setPMIDSelected] = useState("");
  const [uploadFiles, setUploadFiles] = useState(null);
  const formRef = createRef();

  useEffect(() => {
    //if (formRef.current !== null) formRef.current.reset();
    setArticle(props.item);
    setVariant(props.variant);
    if (props.auth) fetch(endpoint + "/supporting/"+props.item.pmId).then(result => result.json())
    .then(data => {
      //console.log(data);
      setResourceData(data);
    })
    fetch(endpoint + "/getSupportingIDs/"+props.item.pmId).then(result => result.json())
    .then(data => {
       setPMIDSupporting(data);
       if (data.length > 0) setPMIDSelected(data[0]);
    })
    bsCustomFileInput.init();
  }, [props.item, props.variant, props.flash, props.auth]);

  const sendData = (formData) => {
    console.log("ADDING ITEM");

    fetch(endpoint + "/articles/addtext/"+article.pmId, {method: "POST", body: formData}).then(response => response.text()).then(data => {
      console.log("Added");
      console.log(data);
      window.location.reload();
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
  }

  const changeHandler = (pmid) => {
    setPMIDSelected(pmid);
    window.open(endpoint + "/getPDF/" + pmid + ".pdf", "_blank");
  }

  const highlight = (text) => {
    let result = text.replaceAll("\n\n", "<br/>").replaceAll("-\n", " ").replaceAll("- \n", " ").replaceAll("\n", " ");
    if (variant != null) result = result.replaceAll(variant.mutation, "<b><mark>"+variant.mutation+"</mark></b>");
    return result;
  }

  return (
    <Container>
      {article.pmId != null &&
      <Card>
        <Card.Header>{variant != null && <b>Curating {variant.gene} {variant.mutation} - </b>} {article.publicationDate != null ? article.publicationDate.substring(0, 10) : null} {article.citation} {article.inSupporting &&
        <b>{"*** Found in supporting document(s) " + article.inSupporting + " ***"}</b>}</Card.Header>
        <Card.Body>
          <Card.Title>{article.pmcId != null && article.fulltext == null ?
              <Button href={endpoint + "/getPDF/" + article.pmId + ".pdf" + (variant != null ? "?terms="+variant.mutation : "")} size="lg" target="_blank" rel="noreferrer">Download
                me!</Button>
              : article.fulltext !== null ?
                  <>
                    <Button
                        href={endpoint + "/getPDF/" + article.pmId + ".pdf" + (props.searchTerms !== undefined ? encodeURI("?terms=" + props.searchTerms) : "")}
                        variant="dark" size="lg" target="_blank" rel="noreferrer">View Main PDF</Button>
                    {PMIDSupporting.length > 0 ? <> {' '}<span
                        className="text-large">or open supporting text(s): </span>
                      <DropdownButton as={Button} id="dropdown-basic-button" title={PMIDSelected}>
                        {PMIDSupporting.map((pmid) => (
                            <Dropdown.Item as="button" eventKey={pmid}
                                           onClick={() => changeHandler(pmid)}>{pmid}</Dropdown.Item>
                        ))}
                      </DropdownButton></> : null}
                  </>
                  : <Form onSubmit={onFormSubmit}><Form.Group controlId="FormFile1"><Form.Control ref={formRef}
                                                                                                  type="file"
                                                                                                  className="custom-file-input"
                                                                                                  onChange={(e) => setUploadFiles(e.target.files)}
                                                                                                  label="Choose PDF file"
                                                                                                  name="attachment"/></Form.Group><Button
                      type="submit" disabled={uploadFiles === null || processing}>Add PDF</Button></Form>}
            <h2>{article.title}</h2></Card.Title>
          <Card.Text>
            <Row>
              <Col><b>AUTHORS:</b> {article.authors != null ? (article.authors.length > 600 ? article.authors.substring(0, 600) + "..." : article.authors) : null}
              </Col>
            </Row>
            <Row>
              <Col>
                <b>PubMed Id:</b> {article.pmId}
              </Col>
              <Col>
                <b>PMC Id:</b> {article.pmcId != null ? article.pmcId : "NONE"}
              </Col>
              <Col>
                <b>doi:</b> {article.doi != null ? props.createLink(article.doi) : "NONE"}
              </Col>
            </Row>
            {resourceData.length > 0 ?
                <>
                  <Row className="justify-content-md-center">
                    <Col xs lg="3"><u>All Supporting Documents</u></Col>
                  </Row>
                  <Row className="justify-content-md-center border border-dark">
                    {resourceData.map((x) => (
                        <Col className="border border-dark"><a
                            href={endpoint + "/supporting/" + x.split(":")[0] + "/" + x.split(":")[1]} target="_blank"
                            rel="noreferrer">{x.split(":")[1]}</a></Col>
                    ))}
                  </Row>
                </> : null}
            <hr/>
            <Row>
              <Col>
                <b><i>{article.pubAbstract != null ? <div dangerouslySetInnerHTML={{__html: highlight(article.pubAbstract)}}/> : null}</i></b>
              </Col>
            </Row>
            <hr/>
            <Row className="justify-content-md-center">
              {article.keywords != null && article.keywords.length > 1 ? article.keywords.split(";").map((a, i) => (
                  <Col xs lg="2">
                    {/*<Card key={`${i}`}><Card.Header>Keyword</Card.Header>
              <Card.Body>
                <Card.Title>{article.substring(article.indexOf(":")+1)}</Card.Title>
              </Card.Body>
            </Card>*/}
                    Keyword: <b>{a.substring(a.indexOf(":") + 1)}</b>
                  </Col>)) : null}
              {article.meshTerms != null && article.meshTerms.length > 1 ? article.meshTerms.split(";").map((a, i) => (
                  <Col xs lg="2">
                    MeSH term: <b>{a.substring(a.lastIndexOf(":") + 1)}</b>
                    {/*<Card key={`${i}`}><Card.Header>MeSH term</Card.Header>
              <Card.Body>
                <Card.Title>{article.substring(article.lastIndexOf(":")+1)}</Card.Title>
              </Card.Body>
            </Card>*/}
                  </Col>)) : null}
              <Row className="justify-content-md-center">
                {article.topics != null && article.topics.length > 1 ? article.topics.split(";").map((av, i) => (
                    <Col xs lg="2">
                      <Card key={`${i}`}>
                        <Card.Header>Filter: {av.substring(0, av.indexOf(":"))}</Card.Header>
                        <Card.Body>
                          <Card.Title>{av.substring(av.lastIndexOf(":") + 1)}</Card.Title>
                        </Card.Body>
                      </Card>
                    </Col>)) : null}
              </Row>
            </Row>
            {article.fulltext != null ?
                <>
                  <Row>
                    <h3><u>Text</u></h3>
                  </Row>
                  <Row>
                    <div style={{fontFamily: "sans-serif"}} dangerouslySetInnerHTML={{__html: highlight(article.fulltext)}}/>
                  </Row>
                </> : null}
          </Card.Text>
        </Card.Body>
      </Card>
      }
    </Container>
  );


}

export default Article;
