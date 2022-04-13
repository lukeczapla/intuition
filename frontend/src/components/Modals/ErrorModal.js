import Modal from "react-bootstrap/Modal";
import Button from "react-bootstrap/Button";

function ErrorModal(props) {
    return (
        <Modal {...props} size="lg" aria-labelledby="contained-modal-title-vcenter" centered>
            <Modal.Header closeButton>
                <Modal.Title id="contained-modal-title-vcenter">
                    Invalid Mutation Code
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <h4>Error Processing Mutation</h4>
                <p>
                    Check that you have formatted your mutations correctly, like A159Q
                </p>
                <p>
                    Item with error: {props.error}
                </p>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={props.onHide}>Close</Button>
            </Modal.Footer>
        </Modal>
    );
}

export default ErrorModal;