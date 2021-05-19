import React, {useEffect, useState, useRef} from "react";
import {useLocation} from "react-router-dom"
import Containter from 'react-bootstrap/Container'
import axios from "axios";
import ClipLoader from 'react-spinners/ClipLoader'
import * as SockJS from 'sockjs-client'
import { Client } from "@stomp/stompjs";

import translate from 'translate-js'
import trans_en from './translate/en'

import Preview from "./Preview"
import RequestList from "./RequestList";
import {Col, Row} from "react-bootstrap";

translate.add(trans_en, 'en')
translate.whenUndefined = (key, locale) => {
    return `${key}:undefined:${locale}`
}

const EvidenceStatus = {
    Accepted: 'Accepted',
    Rejected: 'Rejected',
    Unanswered: 'Unanswered',
    FetchingEvidence: 'FetchingEvidence',
    NoSuchEvidence: 'NoSuchEvidence',
    NoEvidenceChosen: 'NoEvidenceChosen',
    Error: 'Error',
}

const format = (str, args) => {
    console.log("args", args)
    var formatted = str;
    for (var prop in args) {
        var regexp = new RegExp('\\{' + prop + '\\}', 'gi');
        formatted = formatted.replace(regexp, args[prop]);
    }
    return formatted;
}

const App = () => {

    const [evidence, setEvidence] = useState("")
    const [evidenceStatus, setEvidenceStatus] = useState(EvidenceStatus.FetchingEvidence)
    const [evidencesList, setEvidencesList] = useState([])
    const [requestId, setRequestId] = useState("")
    const [backUrl, setBackUrl] = useState("")

    const evidencesListRef = useRef(evidencesList)
    const setEvidencesListRef = useRef(setEvidencesList)

    const search = useLocation().search
    const urlParams = new URLSearchParams(search)
    const requestIdParam = urlParams.get('requestId')
    const backUrlParam = urlParams.get('backUrl')
    const pathName = useLocation().pathname

    const acceptEvidence = () => axios.get(format(window.DO_CONST['previewAcceptEndpoint'], {requestId: requestId}))
        .then(response => {
            setEvidenceStatus(EvidenceStatus.Accepted)
            let urlString = backUrl
            if (urlString === "") {
                if (backUrlParam && backUrlParam !== "") {
                    urlString = backUrlParam
                } else {
                    console.error("no back url provided")
                    setEvidenceStatus(EvidenceStatus.Error)
                }
            }
            let url = new URL(urlString)
            url.searchParams.append('accept', 'true')
            window.location.replace(url.toString());
        })
        .catch(error => {
            console.error("error: ", error)
            setEvidenceStatus(EvidenceStatus.Error)
        })

    const rejectEvidence = () => axios.get(format(window.DO_CONST['previewRejectEndpoint'], {requestId: requestId}))
        .then(response => {
            setEvidenceStatus(EvidenceStatus.Rejected)
            const url = new URL(backUrl)
            url.searchParams.append('accept', 'false')
            window.location.replace(url.toString());
        })
        .catch(error => {
            setEvidenceStatus(EvidenceStatus.Error)
            console.log(error)
        })

    const fetchEvidence = (requestId) => {
        if (requestId && requestId !== "") {
            axios.get(
                format(window.DO_CONST['previewEndpoint'],
                    {requestId: requestId}))
                .then(response => {
                    setEvidence(response.data)
                    setRequestId(requestId)
                    setEvidenceStatus(EvidenceStatus.Unanswered)
                    extractBackUrl(response.data)
                    console.log(response)
                })
                .catch(error => {
                    setEvidenceStatus(EvidenceStatus.NoSuchEvidence)
                    setRequestId(requestId)
                    console.error(error)
                })
        } else {
            setEvidenceStatus(EvidenceStatus.NoEvidenceChosen)
        }
    }

    const extractBackUrl = (evidence) => {
        const parser = new DOMParser()
        const xmlEvidence = parser.parseFromString( evidence, "application/xml")
        const xmlNS = xmlEvidence.createNSResolver(xmlEvidence)

        const dataEvaluatorNode = xmlEvidence.evaluate(
            "//*[local-name() = 'DataEvaluator']",
            xmlEvidence,
            xmlNS,
            XPathResult.ANY_UNORDERED_NODE_TYPE,
            null).singleNodeValue

        if (dataEvaluatorNode.lastChild.localName === "RedirectURL") {
            setBackUrl(dataEvaluatorNode.lastChild.innerHTML)
        }
    }

    const fetchPreviewEvidences = () =>
        axios.get(window.DO_CONST['previewPreviewIdsEndpoint'])
            .then(response => {
                setEvidencesList(response.data)
            })

    const updatePreviewEvidences = (message) => {
        console.log("message: ", message)
        console.log("evidencesList: ", evidencesListRef.current)
        switch (message.action) {
            case("RM"): {
                console.log("rm")
                setEvidencesListRef.current(evidencesListRef.current
                    .filter(value => value !== message.payload))
                break
            }
            case("ADD"): {
                console.log("add")
                if (!evidencesListRef.current.includes(message.payload)) {
                    console.log("inner")
                    setEvidencesListRef.current([...evidencesListRef.current, message.payload])
                }
                break
            }
            default:
                break
        }
        console.log("evidencesList: ", evidencesList.current)
    }

    const newMessage = (message) => {
            console.log("message: ", message.body)
            updatePreviewEvidences(JSON.parse(message.body))
        }

    const client = new Client();
    client.webSocketFactory = () => {
        return new SockJS(`${window.DO_CONST['previewWebsocket']}`)
    }
    client.debug = (str) => {
        console.log(str)
    }
    client.onConnect = function (frame) {
        client.subscribe(`${pathName.replace(/index$/, window.DO_CONST['previewMessages'])}`,
                newMessage)
    };

    client.onStompError = function (frame) {
        console.log('Broker reported error: ' + frame.headers['message']);
        console.log('Additional details: ' + frame.body);
    };

    const subscribeRequestId = () => {
        client.activate()
    }

    useEffect(() => {
        fetchEvidence(requestIdParam)
        fetchPreviewEvidences()
        subscribeRequestId()
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    useEffect(() => {
        evidencesListRef.current = evidencesList
        setEvidencesListRef.current = setEvidencesList
    }, [evidenceStatus, evidencesList])

    const renderSwitch = (evidenceStatus) => {
        switch (evidenceStatus) {
            case EvidenceStatus.FetchingEvidence:
                return <ClipLoader/>
            case EvidenceStatus.NoEvidenceChosen:
                return <RequestList requestIds={evidencesList}
                                    onSelect={fetchEvidence}
                                    translate={translate}/>
            case EvidenceStatus.NoSuchEvidence:
                return <p>no evidence found for request with id: {requestId}</p>
            case EvidenceStatus.Unanswered:
                return <Preview evidence={evidence} evidenceRoot="//*[local-name() = 'CanonicalEvidence']"
                                evidenceIgnore={[]} acceptEvidence={acceptEvidence} rejectEvidence={rejectEvidence}
                                translate={translate}/>
            case EvidenceStatus.Accepted:
                return <p>Evidence accepted</p>
            case EvidenceStatus.Rejected:
                return <p>Evidence rejected</p>
            case EvidenceStatus.Error:
            default:
                return <p>Error occured</p>
        }
    }

    return <Containter>
        <Row>
            <Col><h1>{translate('doTitle')}</h1></Col>
        </Row>
        <Row>
            <Col>
                {renderSwitch(evidenceStatus)}
            </Col>
        </Row>
    </Containter>
}

export default App;
