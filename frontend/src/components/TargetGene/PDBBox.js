import {useState, useEffect, createRef} from 'react';
import * as $3Dmol from '3dmol';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Dropdown from 'react-bootstrap/Dropdown';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import endpoint from '../../endpoint';

const PDBBox = (props) => {
  const [PDBs, setPDBs] = useState(props.items.split(","));
  const [selected, setSelected] = useState(props.items.split(",")[0]);
  const [viewer, setViewer] = useState(null);
  const [items, setItems] = useState(props.items);
  const [boxURL, setBoxURL] = useState("molbox" + props.box);
  const child1 = createRef();

  useEffect(() => {
    const fetchData = async () => {
      const result = await fetch(endpoint + "/getPDB/" + props.items.split(",")[0].toLowerCase() + ".pdb");
      const data = await result.text();
      return data;
    }
    setPDBs(props.items.split(","));
    setSelected(props.items.split(",")[0]);
    fetchData().then(data => {
      let element = child1.current;
      if (element === null) element = document.getElementById(boxURL);
      //console.log(element);
      let config = {backgroundColor: "white"};

      let v;
      if (viewer === null) {
        v = $3Dmol.createViewer(element, config);
        setViewer(v);
      } else {
        v = viewer;
        v.clear();
      }
      v.addModel(data, "pdb");                         /* load data */
      v.setStyle({}, {cartoon: {color: 'spectrum'}});  /* style all atoms */
      v.zoomTo();                                      /* set camera */
      v.render();                                      /* render scene */
      v.zoom(1.2, 1000);                               /* slight zoom */
    });
  }, [props.flash, props.items]);

  const rerender = async (value) => {
    const result = await fetch(endpoint + "/getPDB/" + value.toLowerCase() + ".pdb")
    const data = await result.text();
    let element = child1.current;
    if (element === null) element = document.getElementById(boxURL);
    //console.log(child1);
    let config = {backgroundColor: "white"};

    let v = viewer;
    if (v === null) {
      v = $3Dmol.createViewer(element, config);
      setViewer(v);
    } else {
      v.clear();
    }
    v.addModel(data, "pdb");                       /* load data */
    v.setStyle({}, {cartoon: {color: 'spectrum'}});  /* style all atoms */
    v.zoomTo();                                      /* set camera */
    v.render();                                      /* render scene */
    v.zoom(1.2, 1000);                               /* slight zoom */
  }

  const changeHandler = (value) => {
    setSelected(value);
    rerender(value);
  }

  return (
    <>
    <Container style={{align: "left"}}>
    <Row>
    <DropdownButton id="dropdown-basic-button" title={selected}>
    {PDBs.map((pdb) => (
      <Dropdown.Item as="button" eventKey={pdb} onClick={() => changeHandler(pdb)}>{pdb}</Dropdown.Item>
    ))}
    </DropdownButton>
    </Row>
    <Row>
    <div id={boxURL} style={{width: "400px", height: "400px", position: "relative"}} ref={child1}></div>
    </Row>
    <Row>
    <Button variant="secondary" href={"https://www.rcsb.org/structure/"+selected} target="_blank">Go to RCSB Structure Page</Button>
    </Row>
    </Container>
    </>
  );

};

export default PDBBox;
