import { useLocation, withRouter, BrowserRouter, Router, Switch, Route } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Main from './Main.js';


const Routing = (props) => {

  return (
    <BrowserRouter basename="/intuition" forceRefresh={false} history={props.history}>
      <Switch>
        <Route exact path="/" component={Main} />
	      {/*<Route exact path="/genesearch" component={GeneSearch} />*/}
        <Route exact path="/search" component={Main} />
        <Route exact path="/item" component={Main} />
        <Route exact path="/results" component={Main} />
        <Route exact path="/comment" component={Main} />
        <Route path="/article/:pmId" component={Main} />
        <Route path="/genes/:targetId" component={Main} />
      </Switch>
    </BrowserRouter>
  );

}

export default Routing;
