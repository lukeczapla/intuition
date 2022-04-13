import {useState, useEffect} from 'react';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import ProgressBar from 'react-bootstrap/ProgressBar';
import endpoint from '../../endpoint';

const Resources = (props) => {

    const [memInfo, setMemInfo] = useState("");
    const [osInfo, setOsInfo] = useState("");
    const [startupTime, setStartupTime] = useState("");
    const [jobsRunning, setJobsRunning] = useState("");
    const [jobArray, setJobArray] = useState([]);
    const [percentage, setPercentage] = useState(0);
    const [timer, setTimer] = useState(null);

    const refresh = () => {
        fetch(endpoint + "/resourceUsage").then(result => result.json()).then(data => {
            //console.log("Reading system resources");
            setMemInfo(data.memInfo);
            setOsInfo(data.osInfo);
            setStartupTime(data.startupTime);
            setJobsRunning(data.jobsRunning);
            let ja = data.jobsRunning.split("<br/>");
            setJobArray(ja.slice(0, ja.length-1));
            setPercentage(parseInt(data.memInfo.substring(data.memInfo.indexOf("(%)")+5)));
        });
    }


  /*  useEffect(() => {
        refresh();
        if (timer === null) setTimer(setInterval(refresh, 5000));
        return () => {
            clearInterval(timer);
        }
    }, []); */

    useEffect(() => {
        if (props.auth === false) clearInterval(timer);
        else {
            refresh();
            if (timer === null) {
                let t = setInterval(refresh, 5000);
                setTimer(t);
                props.push(t);
            }
        }
    }, []);

    const stopJob = (line) => {
        let start = line.indexOf("<b>")+3;
        let end = line.indexOf("</b>");
        let threadId = line.substring(start, end);
        fetch(endpoint + (threadId.startsWith("v") ? "/vstop/" : "/stop/") + encodeURI(threadId)).then(data => data.text()).then(result => {
            console.log(result);
            refresh();
        });
    }

    return (
        <>
        <Card>
            <Card.Body>
                <Card.Title>Memory Usage</Card.Title>
                <Card.Text>
                    <div dangerouslySetInnerHTML={{__html: memInfo}}></div>
                    <ProgressBar now={percentage} />
                </Card.Text>
            </Card.Body>
        </Card>
        <Card>
            <Card.Body>
                <Card.Title>Operations and System Information</Card.Title>
                <Card.Text>
                    System started at: {startupTime}
                    <div dangerouslySetInnerHTML={{__html: osInfo}}></div>
                </Card.Text>
            </Card.Body>
        </Card>
        {jobsRunning !== "" &&
        <Card>
            <Card.Body>
                <Card.Title>Calculations Currently Running</Card.Title>
                <Card.Text>
                    Last start up at: {new Date(startupTime).toDateString() + " at " + new Date(startupTime).toTimeString()}
                    {jobArray.length > 0 && jobArray.map((line, i) => (<><div dangerouslySetInnerHTML={{__html: line+"<br/>"}}></div><Button key={`${i}`} onClick={() => stopJob(line)}>Stop Job</Button></>))}
                </Card.Text>
            </Card.Body>
        </Card>
        }
        </>
    );

}

export default Resources;