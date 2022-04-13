import {useEffect, useState} from "react";
import endpoint from "../../endpoint";
import Modal from "react-bootstrap/Modal";
import Form from "react-bootstrap/Form";
import Alert from "react-bootstrap/Alert";
import Button from "react-bootstrap/Button";

const useEnterKeyListener = (props) => {
    useEffect(() => {
        //https://stackoverflow.com/a/59147255/828184
        const listener = (event) => {
            if (event.code === "Enter" || event.code === "NumpadEnter") {
                handlePressEnter();
            }
        };

        document.addEventListener("keydown", listener);

        return () => {
            document.removeEventListener("keydown", listener);
        };
    });

    const handlePressEnter = () => {
        //https://stackoverflow.com/a/54316368/828184
        const mouseClickEvents = ["mousedown", "click", "mouseup"];
        function simulateMouseClick(element) {
            mouseClickEvents.forEach((mouseEventType) => {
                if (element !== null) element.dispatchEvent(
                    new MouseEvent(mouseEventType, {
                        view: window,
                        bubbles: true,
                        cancelable: true,
                        buttons: 1,
                    })
                )
            });
        }

        var element = document.querySelector(props.querySelectorToExecuteClick);
        simulateMouseClick(element);
    };
}

const LoginModal = (props) => {

    const [validated, setValidated] = useState(true);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    useEffect(() => {
        props.auth();
    }, []);

    const update = (e) => {
        if (e.target.name === 'email') setEmail(e.target.value);
        if (e.target.name === 'password') setPassword(e.target.value);
    }

    useEnterKeyListener({querySelectorToExecuteClick: "#submitButton123"});

    const loginLDAP = () => {
        if (email === '' || password === '') return;
        let user = {
            emailAddress: email.includes("@") ? email : email+"@mskcc.org",
            password: password
        };
        fetch(endpoint+"/conf/user", {method: "POST", headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(user)})
            .then(result => result.text())
            .then(data => {
                user.password = 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx';  // try to overwrite the password field for security
                //console.log(data);
                if (data.startsWith("Finished") || data.startsWith("Created")) {
                    setPassword('xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
                    props.onClose();
                    props.onAuthenticate();
                }
                else {
                    //console.log("show message");
                    setValidated(false);
                }
            });
    }

    return (
        <Modal show={props.show} onHide={props.onClose} backdrop="static" size="lg" keyboard={true} centered>
            <Modal.Header closeButton>
                <Modal.Title>Log In to Curation Assistant</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <Form>
                <Form.Group controlId="formBasicEmail">
                    <Form.Label>Email address (or username)</Form.Label>
                    <Form.Control type="email" placeholder="Enter mskcc.org email" value={email} name="email" onChange={update} autoComplete="username" />
                    <Form.Text className="text-muted">
                        These are the same as the login credentials to your emails and other MSK resources
                    </Form.Text>
                </Form.Group>

                <Form.Group controlId="formBasicPassword">
                    <Form.Label>Password</Form.Label>
                    <Form.Control type="password" placeholder="Password" value={password} name="password" onChange={update} autoComplete="current-password" />
                </Form.Group>
                </Form>

                <Alert show={!validated} variant="danger">Log in failed: Please check your email address (must be mskcc.org) and password</Alert>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={props.onClose}>Close</Button>
                <Button variant="primary" id="submitButton123" onClick={() => loginLDAP()}>Sign In</Button>
            </Modal.Footer>
        </Modal>
    );
}

export default LoginModal;