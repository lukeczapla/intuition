import {useState} from 'react';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Popover from 'react-bootstrap/Popover';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import Button from 'react-bootstrap/Button';
import Spinner from 'react-bootstrap/Spinner';
import InputGroup from 'react-bootstrap/InputGroup';
import FormControl from 'react-bootstrap/FormControl';
import '../App.scss';
import '../bootstrap.min.css';

const Simulation = (props) => {

  const [key, setKey] = useState('home');

  const popover = (
    <Popover id="popover-basic">
      <Popover.Title as="h3">Welcome to the AbDB!</Popover.Title>
      <Popover.Content>
        Suggestions - enter the catalog number (for an antibody) to get the record and comments, or enter the name of a gene to browse Ab records for that target.
      </Popover.Content>
    </Popover>
  );

  return (
    <>
    <div className="App">
    <header className="bg-muted">
    <OverlayTrigger trigger="click" placement="right" overlay={popover}>
      <Button variant="success">Simulation Tool</Button>
    </OverlayTrigger>
    </header>
    </div>
    <div class="searchHeader">
    <div className="searchBody">

    </div>
    </div>
    <Tabs defaultActiveKey="home" activeKey={key} onSelect={(k) => setKey(k)} id="menutabs">
      <Tab eventKey="home" title="Main">
      </Tab>
      <Tab eventKey="create" title="Run Simulation">
      </Tab>
      <Tab eventKey="analyze" title="Analyze Simulations">
      </Tab>
      <Tab eventKey="plot" title="Plot Results">
      </Tab>
    </Tabs>
    </>
  );
}

export default Simulation;
