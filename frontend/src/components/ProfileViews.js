import React, {useState} from "react";
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import Image from 'react-bootstrap/Image';
import Dropdown from "react-bootstrap/Dropdown";


export const ProfileContent = (props) => {
    const [showModal, setShowModal] = useState(false);

    return (
        <><Dropdown.Item as="button" onClick={() => setShowModal(true)}>{props.user.firstName}'s Profile</Dropdown.Item>
            {showModal ? <ProfileData user={props.user} show={showModal} onHide={() => setShowModal(false)}/> : null}</>
    );
};


export const ProfileData = (props) => {
//    console.log(props.graphData);

    return (
      <>{props.user != null ?
        <Modal {...props} size="lg" aria-labelledby="contained-modal-title-vcenter" centered>
          <Modal.Header closeButton>
            <Modal.Title id="contained-modal-title-vcenter">
              {props.user.emailAddress}
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <h4>{props.user.firstName} {props.user.lastName}</h4>
            <p>
              ID: {props.user.emailAddress}
            </p>
            <p>
              Group/PI: {props.user.group}
              {props.user.streetAddress != null ? props.user.streetAddress : null}
            </p>
            {props.user.contentType != null && props.user.contentType != null && props.user.contentType.startsWith("image") ?
            <Image style={{height:"20%", border: "1px solid black"}} src={"data:"+props.user.contentType+";base64,"+props.user.image.data} /> : null}
          </Modal.Body>
          <Modal.Footer>
            <Button onClick={props.onHide}>Close</Button>
          </Modal.Footer>
        </Modal> : null}</>
    );
};
