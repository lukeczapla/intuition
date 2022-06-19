import {useEffect, useState} from "react";
import {GoogleLogin} from 'react-google-login';
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
    const clientId = '613395107842-nmr1dthih3c5ibcfcsrrkq61ef838ks8.apps.googleusercontent.com';

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

    const loginGoogle = (res) => {
        //if (email === '' || password === '') return;
        let user = {
            emailAddress: res.profileObj.email,
            tokenId: res.getAuthResponse().id_token,
            imageUrl: res.profileObj.imageUrl
        };
        console.log(res.profileObj.tokenId);
        if (typeof res.profileObj.name == "string" && res.profileObj.name.indexOf(" ") !== -1) {
            user.firstName = res.profileObj.name.substring(0, res.profileObj.name.lastIndexOf(" "));
            user.lastName = res.profileObj.name.substring(res.profileObj.name.lastIndexOf(" ")+1);
        } else user.firstName = res.profileObj.name;
        fetch(endpoint+"/conf/usergoogle", {method: "POST", headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(user)})
            .then(result => result.text())
            .then(data => {
                //user.password = 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx';  // try to overwrite the password field for security
                //console.log(data);
                if (data.startsWith("Finished") || data.startsWith("Created")) {
                    //setPassword('xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
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
        <Modal show={props.show} onHide={props.onClose} backdrop="static" size="md" keyboard={true} centered>
            <Modal.Header closeButton>
                <Modal.Title>Log In to Curation Assistant</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <GoogleLogin clientId={clientId} buttonText="Google Login" onSuccess={loginGoogle} cookiePolicy={'single_host_origin'} isSignedIn={true} />
            </Modal.Body>
        </Modal>
    );
}

export default LoginModal;