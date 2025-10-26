import { useLocation, useHistory, withRouter, BrowserRouter, Router, Switch, Route } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { GoogleOAuthProvider } from "@react-oauth/google";
import Intuition from './Intuition.js';

const Main = (props) => {
  const [pageMax, setPageMax] = useState(0);
  const [page, setPage] = useState(0);
  const [key, setKey] = useState("");
  const [state, setState] = useState([]);
  const [states, setStates] = useState([]);
  const [locationKeys, setLocationKeys] = useState([]);
  const location = useLocation();
  const history = useHistory();

  const handleChange = (newkey, state, linkp) => {
    //console.log("MOVING ALONG");
    history.push("/"+linkp, state);
  }

  useEffect(() => {
    return () => {
      //console.log(page);
      //console.log(props);
      //console.log(location.key);
      //console.log(states);
      //console.log(locationKeys);
      if (history.action === 'PUSH') {
        //console.log("PUSH");
        if (page > locationKeys.length) {
          locationKeys.push(location.key);
          states.push(location.state);
        } else {
          locationKeys[page] = location.key;
          states[page] = location.state;
        }
        let newpage = page+1;
        setPage(newpage);
        setPageMax(newpage);
        setStates(states);
        setLocationKeys(locationKeys);
      }
      if (history.action === 'POP') {
        //console.log("POP");
        if (page < pageMax && locationKeys[page+1] === location.key) {
          //Go forward
          //console.log(page+1);
          setState(states[page+1]);
          setPage(page+1);
          //console.log("go forward!");
        } else if (page - 1 >= 0) {
          //setLocationKeys(([ _, ...keys]) => keys);
          let newpage = page-1;
          setPage(newpage);
          //console.log("newpage = ", newpage);
          if (newpage >= 0) setState(states[newpage]);
          //console.log("go back!");
        }
        if (history.action === 'REPLACE') {
          console.log("REPLACE");
        }
      }
    };
  });

  return (
  <GoogleOAuthProvider clientId={'613395107842-nmr1dthih3c5ibcfcsrrkq61ef838ks8.apps.googleusercontent.com'}>
  <Intuition {...props} state={state} onMove={handleChange} />
  </GoogleOAuthProvider>
  )
  
  ;

}

export default withRouter(Main);
