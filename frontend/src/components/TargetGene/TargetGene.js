import React, { useState, useEffect } from 'react';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import PDBBox from './PDBBox.js';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import endpoint from '../../endpoint';

const TargetGene = (props) => {
  const [item, setItem] = useState(props.item);
  const [box, setBox] = useState(7000);//parseInt((Math.random()*10000)+""));
  const [variants, setVariants] = useState([]);
  const [flashPDB, setFlashPDB] = useState(0);

  useEffect(() => {
    const fetchData = async () => {
      const response = await fetch(endpoint + "/getVariantsByGene/"+props.item.symbol);
      const data = await response.json();
      setVariants(data);
    }
    setItem(props.item);
    if (props.auth) fetchData();
  }, [props.item, props.flash, props.auth]);

  useEffect(() => {
    setFlashPDB(oldCount => oldCount + 1);
    //setFlashPDB(flashPDB+1);
  }, [props.flash]);

  const process = (text) => {
    //console.log(props.item);
    let re = /(PubMed:\d{7,8})/i;
    let parts = text.split(re);
    let j = 1;
    for (let i = 1; i < parts.length; i += 2) {
      parts[i] = <a key={'link'+j} rel="noreferrer" target="_blank" href={'https://pubmed.ncbi.nlm.nih.gov/'+parts[i].substring(7)}>[{j+''}]</a>
      j++;
    }
    return parts;
  }

  const parseSyn = (text) => {
    let items = text.split(";");
    let result = items[0];
    for (let i = 1; i < items.length; i++) {
      result += ", " + items[i];
    }
    return result;
  }

  const leftOver = (item) => {
    return item.antibodyCount - item.monoclonalCount - item.polyclonalCount - item.recombinantCount - item.polyclonalAntigenPurifiedCount - item.otherCount;
  }

/*  useEffect(() => {
    setItem(props.item);
  }, [props.item]);
*/
  return (<>{item.symbol != null &&
        <Card>
          <Card.Header>
            <h3>{item.symbol} - {item.name} [<i>pubmedScore:</i> {Math.round(item.pubmedScore)}]</h3>
          </Card.Header>
          <Card.Text>
            <Container>
              <Row><h4>{item.synonyms != null && item.synonyms.length > 0 ?
                  <p><b>synonyms: </b>{parseSyn(item.synonyms)}</p> : null}</h4></Row>
              <Row className="justify-content-md-center"><Col>{item.uniprotFunction.length < 500 ?
                  <h4><i>{process(item.uniprotFunction)}</i></h4> :
                  <i>{process(item.uniprotFunction)}</i>}</Col></Row>
              <Row className="justify-content-md-center"><Col><h4><b>Family:</b> {item.family}</h4></Col></Row>
              <Row><h4><b>Variants:</b></h4></Row>
              <Row className="justify-content-md-center" style={{borderStyle: "ridge"}}>
                {props.auth && variants.length > 0 ? variants.filter(v => v.consensusPMIDs !== null && v.mutation !== null && v.mutation !== '').map(v => (v.articlesTier1 != null && v.articlesTier1.length > 0 &&
                    <Col style={{borderStyle: "ridge"}}><Button size="sm"
                                                                onClick={() => props.push(v)}>{v.mutation} {v.cancerTypes !== null ? " in " + v.cancerTypes : ""} {v.drugs !== null ? " with " + v.drugs : ""}</Button></Col>)) : null}
              </Row>
              <Row className="justify-content-md-center">{item.cBioPortal !== undefined && item.cBioPortal ?
                  <Col><b>cBioPortal link: </b> <a target="_blank" rel="noreferrer"
                                                   href={'https://www.cbioportal.org/results/mutations?cancer_study_list=msk_impact_2017&Z_SCORE_THRESHOLD=2.0&RPPA_SCORE_THRESHOLD=2.0&profileFilter=mutations%2Cfusion%2Ccna&case_set_id=msk_impact_2017_cnaseq&gene_list=' + item.symbol + '&geneset_list=%20&tab_index=tab_visualize&Action=Submit'}>{item.symbol}
                  </a></Col> : null}
              </Row>
              <Row className="justify-content-md-center">
                {item.uniprotID !== undefined && item.uniprotID.length > 0 ?
                    <Col><b>Uniprot gene page: </b> <a target="_blank" rel="noreferrer"
                                                       href={'https://www.uniprot.org/uniprot/' + item.uniprotID}>{item.uniprotID}</a>
                    </Col> : null}
              </Row>
              <Row className="justify-content-md-center">
                <Col>
                  {item.ncbigeneURL !== undefined && item.ncbigeneURL.length > 0 ?
                      <><b>NCBI gene page: </b> <a target="_blank" rel="noreferrer"
                                                   href={item.ncbigeneURL}>{item.ncbigeneURL.substring(item.ncbigeneURL.lastIndexOf("/") + 1)}</a>
                      </> : null}
                </Col>
              </Row>
              <Row className="justify-content-md-center">
                <Col>
                  <b>GeneCards page: </b><a target="_blank" rel="noreferrer"
                                            href={"https://www.genecards.org/cgi-bin/carddisp.pl?gene=" + item.symbol}>{item.symbol}</a>
                </Col>
              </Row>
              <Row className="justify-content-md-center">
                <Col>
                  <b>COSMIC page: </b><a target="_blank" rel="noreferrer"
                                         href={"https://cancer.sanger.ac.uk/cosmic/gene/analysis?ln=" + item.symbol}>{item.symbol}</a>
                </Col>
              </Row>
              <Row>
                {item.pdb_IDs !== null && item.pdb_IDs !== "" && props.auth ?
                    <PDBBox flash={flashPDB} items={item.pdb_IDs} box={box}/> : null}
                <div style={{width: "100%", margin: "auto"}}>
                </div>
              </Row>
            </Container>
          </Card.Text>
        </Card>
  }</>);

}

export default TargetGene;
