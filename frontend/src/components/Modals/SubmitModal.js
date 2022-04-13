import Modal from "react-bootstrap/Modal";
import Button from "react-bootstrap/Button";

const SubmitModal = (props) => {

    return (
        <Modal show={props.show} onHide={props.onClose} backdrop="static" size="lg" keyboard={true} centered>
            <Modal.Header closeButton>
                <Modal.Title>Download task is submitted</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                The download job {props.job} is in progress and the status can be monitored in the Resources tab.
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={props.onClose}>Close</Button>
            </Modal.Footer>
        </Modal>
    );

}

export default SubmitModal;