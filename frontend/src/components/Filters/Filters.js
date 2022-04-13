import {useState, useEffect} from 'react';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import FloatingLabel from 'react-bootstrap/FloatingLabel';

const Filters = (props) => {

  const [groups, setGroups] = useState(["--Blank: Select a Type--", "Gene", "Mutation", "Drug", "Cancer Type"]);
  const [groupItems, setGroupItems] = useState([["--Choose type above--"], null, null, null, null]);
  const [nFilters, setNFilters] = useState(5);
  const [groupValues, setGroupValues] = useState([0, 0, 0, 0, 0]);
  const [groupItemValues, setGroupItemValues] = useState([0, 0, 0, 0, 0]);

  useEffect(() => {
    groupItems[1] = props.groupItems[0];
    groupItems[2] = props.groupItems[1];
    groupItems[3] = props.groupItems[2];
    groupItems[4] = props.groupItems[3];
    groupItems[5] = props.groupItems[4];
  }, [props.groupItems]);

  useEffect(() => {
    props.onFilterCall([groupValues, groupItemValues]);
    let newValues = [0,0,0,0,0];
    let newItems = [0,0,0,0,0];
    setGroupValues(newValues);
    setGroupItemValues(newItems);
  }, [props.count]);

  useEffect(() => {
    let newValues = [0,0,0,0];
    let newItems = [0,0,0,0];
    setGroupValues(newValues);
    setGroupItemValues(newItems);
  }, [props.clear]);

  const getGroupValueItems = () => {
    return(
      <>
      <option value="0" key="0">{groups[0]}</option>
      <option value="1" key="1">{groups[1]}</option>
      <option value="2" key="2">{groups[2]}</option>
      <option value="3" key="3">{groups[3]}</option>
      <option value="4" key="4">{groups[4]}</option>
      </>
    )
  }

  const getGroupItems = (x) => {
    if (groupItems[x] !== null) return groupItems[x].map((g,i) => <option value={`${i}`} key={`${i}`}>{g}</option>);
  }

  const changeEvent = (e) => {
    if (e.target.name === "groupValues") {
      let newValues = [];
      for (let i = 0; i < groupValues.length; i++) newValues.push(groupValues[i]);
      newValues[parseInt(e.target.id)] = e.target.value;
      setGroupValues(newValues);
    }
    else if (e.target.name === "groupItemValues") {
      let newValues = [];
      for (let i = 0; i < groupItemValues.length; i++) newValues.push(groupItemValues[i]);
      newValues[parseInt(e.target.id)] = e.target.value;
      setGroupItemValues(newValues);
    }
  }

  return(
    <>
    <Row>
    {groupValues.map((g,i) => <Col><u style={{color: "white"}}>Filter #{i+1}</u></Col>)}
    </Row>
    <Row>
    {groupValues.map((g,i) => <Col><FloatingLabel controlId="floatingSelect" label="Pick main category"><Form.Select id={`${i}`} name="groupValues" onChange={(e) => changeEvent(e)} value={groupValues[i]}>
    {getGroupValueItems()}</Form.Select></FloatingLabel></Col>)}
    </Row>
    <Row>
    {groupItemValues.map((g,i) => <Col><Form.Select id={`${i}`} name="groupItemValues" value={groupItemValues[i]} onChange={(e) => changeEvent(e)}>{getGroupItems(groupValues[i])}</Form.Select></Col>)}
    </Row>
    </>
  );

}

export default Filters;
