--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.19
-- Dumped by pg_dump version 9.5.19

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: clinlims; Type: SCHEMA; Schema: -; Owner: clinlims
--

CREATE SCHEMA clinlims;


ALTER SCHEMA clinlims OWNER TO clinlims;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: tablefunc_crosstab_2; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.tablefunc_crosstab_2 AS (
	row_name text,
	category_1 text,
	category_2 text
);


ALTER TYPE public.tablefunc_crosstab_2 OWNER TO postgres;

--
-- Name: tablefunc_crosstab_3; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.tablefunc_crosstab_3 AS (
	row_name text,
	category_1 text,
	category_2 text,
	category_3 text
);


ALTER TYPE public.tablefunc_crosstab_3 OWNER TO postgres;

--
-- Name: tablefunc_crosstab_4; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.tablefunc_crosstab_4 AS (
	row_name text,
	category_1 text,
	category_2 text,
	category_3 text,
	category_4 text
);


ALTER TYPE public.tablefunc_crosstab_4 OWNER TO postgres;

--
-- Name: connectby(text, text, text, text, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.connectby(text, text, text, text, integer) RETURNS SETOF record
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'connectby_text';


ALTER FUNCTION public.connectby(text, text, text, text, integer) OWNER TO postgres;

--
-- Name: connectby(text, text, text, text, integer, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.connectby(text, text, text, text, integer, text) RETURNS SETOF record
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'connectby_text';


ALTER FUNCTION public.connectby(text, text, text, text, integer, text) OWNER TO postgres;

--
-- Name: connectby(text, text, text, text, text, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.connectby(text, text, text, text, text, integer) RETURNS SETOF record
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'connectby_text_serial';


ALTER FUNCTION public.connectby(text, text, text, text, text, integer) OWNER TO postgres;

--
-- Name: connectby(text, text, text, text, text, integer, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.connectby(text, text, text, text, text, integer, text) RETURNS SETOF record
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'connectby_text_serial';


ALTER FUNCTION public.connectby(text, text, text, text, text, integer, text) OWNER TO postgres;

--
-- Name: crosstab(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.crosstab(text) RETURNS SETOF record
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'crosstab';


ALTER FUNCTION public.crosstab(text) OWNER TO postgres;

--
-- Name: crosstab(text, integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.crosstab(text, integer) RETURNS SETOF record
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'crosstab';


ALTER FUNCTION public.crosstab(text, integer) OWNER TO postgres;

--
-- Name: crosstab(text, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.crosstab(text, text) RETURNS SETOF record
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'crosstab_hash';


ALTER FUNCTION public.crosstab(text, text) OWNER TO postgres;

--
-- Name: crosstab2(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.crosstab2(text) RETURNS SETOF public.tablefunc_crosstab_2
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'crosstab';


ALTER FUNCTION public.crosstab2(text) OWNER TO postgres;

--
-- Name: crosstab3(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.crosstab3(text) RETURNS SETOF public.tablefunc_crosstab_3
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'crosstab';


ALTER FUNCTION public.crosstab3(text) OWNER TO postgres;

--
-- Name: crosstab4(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.crosstab4(text) RETURNS SETOF public.tablefunc_crosstab_4
    LANGUAGE c STABLE STRICT
    AS '$libdir/tablefunc', 'crosstab';


ALTER FUNCTION public.crosstab4(text) OWNER TO postgres;

--
-- Name: normal_rand(integer, double precision, double precision); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.normal_rand(integer, double precision, double precision) RETURNS SETOF double precision
    LANGUAGE c STRICT
    AS '$libdir/tablefunc', 'normal_rand';


ALTER FUNCTION public.normal_rand(integer, double precision, double precision) OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: action; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.action (
    id numeric(10,0) NOT NULL,
    code character varying(10) NOT NULL,
    description character varying(256) NOT NULL,
    type character varying(10) NOT NULL,
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.action OWNER TO clinlims;

--
-- Name: action_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.action_seq
    START WITH 45
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.action_seq OWNER TO clinlims;

--
-- Name: address_part; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.address_part (
    id numeric(10,0) NOT NULL,
    part_name character varying(20) NOT NULL,
    display_order numeric(4,0),
    display_key character varying(20)
);


ALTER TABLE clinlims.address_part OWNER TO clinlims;

--
-- Name: TABLE address_part; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.address_part IS 'Holds the different parts of an address';


--
-- Name: COLUMN address_part.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.address_part.id IS 'Unique id genereated from address_part seq';


--
-- Name: COLUMN address_part.part_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.address_part.part_name IS 'What part of the address is this, street, commune state etc.';


--
-- Name: COLUMN address_part.display_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.address_part.display_order IS 'The order in which they are listed in the standardard address format';


--
-- Name: COLUMN address_part.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.address_part.display_key IS 'The display key for localization';


--
-- Name: address_part_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.address_part_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.address_part_seq OWNER TO clinlims;

--
-- Name: analysis; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.analysis (
    id numeric(10,0) NOT NULL,
    sampitem_id numeric(10,0),
    test_sect_id numeric(10,0),
    test_id numeric(10,0),
    revision numeric,
    status character varying(1),
    started_date timestamp without time zone,
    completed_date timestamp without time zone,
    released_date timestamp without time zone,
    printed_date timestamp without time zone,
    is_reportable character varying(1),
    so_send_ready_date timestamp without time zone,
    so_client_reference character varying(240),
    so_notify_received_date timestamp without time zone,
    so_notify_send_date timestamp without time zone,
    so_send_date timestamp without time zone,
    so_send_entry_by character varying(240),
    so_send_entry_date timestamp without time zone,
    analysis_type character varying(10) NOT NULL,
    lastupdated timestamp(6) without time zone,
    parent_analysis_id numeric(10,0),
    parent_result_id numeric(10,0),
    reflex_trigger boolean DEFAULT false,
    status_id numeric(10,0),
    entry_date timestamp with time zone,
    panel_id numeric(10,0),
    referred_out boolean DEFAULT false,
    type_of_sample_name character varying(40),
    corrected boolean DEFAULT false
);


ALTER TABLE clinlims.analysis OWNER TO clinlims;

--
-- Name: COLUMN analysis.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.id IS 'Sequential number';


--
-- Name: COLUMN analysis.sampitem_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.sampitem_id IS 'Sample source write in if not already defined';


--
-- Name: COLUMN analysis.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.test_id IS 'Sequential value assigned on insert';


--
-- Name: COLUMN analysis.revision; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.revision IS 'revision number';


--
-- Name: COLUMN analysis.status; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.status IS 'Analysis Status; logged in, initiated, completed, released';


--
-- Name: COLUMN analysis.started_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.started_date IS 'Date and time analysis started';


--
-- Name: COLUMN analysis.completed_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.completed_date IS 'Date and time analysis completed';


--
-- Name: COLUMN analysis.released_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.released_date IS 'Date and time analysis was released; basically verified and ready to report';


--
-- Name: COLUMN analysis.printed_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.printed_date IS 'Date and time analysis was last printed for sending out';


--
-- Name: COLUMN analysis.is_reportable; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.is_reportable IS 'Indicates if this analysis should be reported';


--
-- Name: COLUMN analysis.so_send_ready_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.so_send_ready_date IS 'Send out ready date';


--
-- Name: COLUMN analysis.so_notify_received_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.so_notify_received_date IS 'Date that send out facility notificed MDH that they had received the specimen';


--
-- Name: COLUMN analysis.so_notify_send_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.so_notify_send_date IS 'Date that MDH sent out the specimen to a sendout facility';


--
-- Name: COLUMN analysis.so_send_entry_by; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.so_send_entry_by IS 'User name who entered sendout';


--
-- Name: COLUMN analysis.reflex_trigger; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.reflex_trigger IS 'True if this analysis has triggered a reflex test';


--
-- Name: COLUMN analysis.status_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.status_id IS 'foriegn key to status of analysis ';


--
-- Name: COLUMN analysis.entry_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.entry_date IS 'Date on which the results for this analysis was first entered';


--
-- Name: COLUMN analysis.panel_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.panel_id IS 'If this analysis is part of a panel then this is the id';


--
-- Name: COLUMN analysis.referred_out; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.referred_out IS 'True if this analysis has been referred out, false otherwise';


--
-- Name: COLUMN analysis.type_of_sample_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.type_of_sample_name IS 'Used to support arbitrary sample types.  Usually from type_of_sample table but may be a dictionary value or a response to ''other''';


--
-- Name: COLUMN analysis.corrected; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analysis.corrected IS 'Signifies that the result has been updated since the last patient report was generated';


--
-- Name: analysis_qaevent; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.analysis_qaevent (
    id numeric(10,0) NOT NULL,
    qa_event_id numeric(10,0),
    analysis_id numeric(10,0),
    lastupdated timestamp(6) without time zone,
    completed_date timestamp without time zone
);


ALTER TABLE clinlims.analysis_qaevent OWNER TO clinlims;

--
-- Name: analysis_qaevent_action; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.analysis_qaevent_action (
    id numeric(10,0) NOT NULL,
    analysis_qaevent_id numeric(10,0) NOT NULL,
    action_id numeric(10,0) NOT NULL,
    created_date timestamp without time zone NOT NULL,
    lastupdated timestamp(6) without time zone,
    sys_user_id numeric(10,0)
);


ALTER TABLE clinlims.analysis_qaevent_action OWNER TO clinlims;

--
-- Name: analysis_qaevent_action_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.analysis_qaevent_action_seq
    START WITH 221
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.analysis_qaevent_action_seq OWNER TO clinlims;

--
-- Name: analysis_qaevent_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.analysis_qaevent_seq
    START WITH 326
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.analysis_qaevent_seq OWNER TO clinlims;

--
-- Name: analysis_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.analysis_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.analysis_seq OWNER TO clinlims;

--
-- Name: analyte; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.analyte (
    id numeric(10,0) NOT NULL,
    analyte_id numeric(10,0),
    name character varying(60),
    is_active character varying(1),
    external_id character varying(20),
    lastupdated timestamp(6) without time zone,
    local_abbrev character varying(10)
);


ALTER TABLE clinlims.analyte OWNER TO clinlims;

--
-- Name: COLUMN analyte.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyte.name IS 'Name of analyte';


--
-- Name: COLUMN analyte.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyte.is_active IS 'Flag indicating if the test is active';


--
-- Name: COLUMN analyte.external_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyte.external_id IS 'External ID such as CAS #';


--
-- Name: analyte_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.analyte_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.analyte_seq OWNER TO clinlims;

--
-- Name: analyzer; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.analyzer (
    id numeric(10,0) NOT NULL,
    scrip_id numeric(10,0),
    name character varying(20),
    machine_id character varying(20),
    description character varying(60),
    analyzer_type character varying(30),
    is_active boolean,
    location character varying(60),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.analyzer OWNER TO clinlims;

--
-- Name: COLUMN analyzer.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer.name IS 'Short name for analyzer';


--
-- Name: COLUMN analyzer.machine_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer.machine_id IS 'id which uniquely matches a machine, descriminates between duplicate analyzers';


--
-- Name: COLUMN analyzer.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer.description IS 'analyzer description';


--
-- Name: COLUMN analyzer.analyzer_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer.analyzer_type IS 'Type of analyzer: Mass Spec, HPLC, etc.';


--
-- Name: COLUMN analyzer.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer.is_active IS 'Flag indicating if the analyzer is active';


--
-- Name: COLUMN analyzer.location; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer.location IS 'Location of analyzer';


--
-- Name: analyzer_result_status; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.analyzer_result_status (
    id numeric(10,0) NOT NULL,
    name character varying(30) NOT NULL,
    description character varying(60)
);


ALTER TABLE clinlims.analyzer_result_status OWNER TO clinlims;

--
-- Name: analyzer_results; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.analyzer_results (
    id numeric(10,0) NOT NULL,
    analyzer_id numeric(10,0) NOT NULL,
    accession_number character varying(20) NOT NULL,
    test_name character varying NOT NULL,
    result character varying NOT NULL,
    units character varying,
    status_id numeric(10,0) DEFAULT 1 NOT NULL,
    iscontrol boolean DEFAULT false NOT NULL,
    lastupdated timestamp(6) without time zone,
    read_only boolean DEFAULT false,
    duplicate_id numeric(10,0),
    positive boolean DEFAULT false,
    complete_date timestamp with time zone,
    test_result_type character varying(1),
    test_id numeric(10,0)
);


ALTER TABLE clinlims.analyzer_results OWNER TO clinlims;

--
-- Name: TABLE analyzer_results; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.analyzer_results IS 'A holding table for analyzer results ';


--
-- Name: COLUMN analyzer_results.analyzer_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.analyzer_id IS 'Reference to analyzer table';


--
-- Name: COLUMN analyzer_results.accession_number; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.accession_number IS 'The display version of the accession number.  May be either the extended or normal accession number';


--
-- Name: COLUMN analyzer_results.test_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.test_name IS 'The test name, if a mapping is found then the mapping will be used, if not then the analyzer test name will be useds';


--
-- Name: COLUMN analyzer_results.result; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.result IS 'The result of the test, the meaning depends on the test itself';


--
-- Name: COLUMN analyzer_results.units; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.units IS 'The units as sent from the analyzer';


--
-- Name: COLUMN analyzer_results.status_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.status_id IS 'The status for the this analyzer result';


--
-- Name: COLUMN analyzer_results.iscontrol; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.iscontrol IS 'Is this result a control';


--
-- Name: COLUMN analyzer_results.read_only; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.read_only IS 'Is this result read only';


--
-- Name: COLUMN analyzer_results.duplicate_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.duplicate_id IS 'Reference to another analyzer result with the same analyzer and analyzer test';


--
-- Name: COLUMN analyzer_results.positive; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.positive IS 'Is the test positive';


--
-- Name: COLUMN analyzer_results.complete_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.complete_date IS 'The time stamp for when the analyzsis was done';


--
-- Name: COLUMN analyzer_results.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_results.test_id IS 'test_id from test';


--
-- Name: analyzer_results_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.analyzer_results_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.analyzer_results_seq OWNER TO clinlims;

--
-- Name: analyzer_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.analyzer_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.analyzer_seq OWNER TO clinlims;

--
-- Name: analyzer_test_map; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.analyzer_test_map (
    analyzer_id numeric(10,0) NOT NULL,
    analyzer_test_name character varying(30) NOT NULL,
    test_id numeric(10,0),
    lastupdated timestamp with time zone DEFAULT '2011-12-27 15:41:40.194936-08'::timestamp with time zone
);


ALTER TABLE clinlims.analyzer_test_map OWNER TO clinlims;

--
-- Name: TABLE analyzer_test_map; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.analyzer_test_map IS 'Maps the analyzers names to the tests in database';


--
-- Name: COLUMN analyzer_test_map.analyzer_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_test_map.analyzer_id IS 'foriegn key to analyzer table';


--
-- Name: COLUMN analyzer_test_map.analyzer_test_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_test_map.analyzer_test_name IS 'The name the analyzer uses for the test';


--
-- Name: COLUMN analyzer_test_map.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.analyzer_test_map.test_id IS 'foriegn key to test table';


--
-- Name: attachment; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.attachment (
    id numeric(10,0) NOT NULL,
    attach_type character varying(20),
    filename character varying(60),
    description character varying(80),
    storage_reference character varying(255)
);


ALTER TABLE clinlims.attachment OWNER TO clinlims;

--
-- Name: attachment_item; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.attachment_item (
    id numeric(10,0) NOT NULL,
    reference_id numeric,
    reference_table_id numeric,
    attachment_id numeric
);


ALTER TABLE clinlims.attachment_item OWNER TO clinlims;

--
-- Name: barcode_label_info; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.barcode_label_info (
    id numeric(20,0) NOT NULL,
    code character varying(20) NOT NULL,
    numprinted numeric(3,0),
    type character varying(20),
    lastupdated timestamp without time zone NOT NULL
);


ALTER TABLE clinlims.barcode_label_info OWNER TO clinlims;

--
-- Name: barcode_label_info_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.barcode_label_info_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.barcode_label_info_seq OWNER TO clinlims;

--
-- Name: city_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.city_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.city_seq OWNER TO clinlims;

--
-- Name: city_state_zip; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.city_state_zip (
    id numeric(10,0),
    city character varying(30),
    state character varying(2),
    zip_code character varying(10),
    county_fips numeric(3,0),
    county character varying(25),
    region_id numeric(3,0),
    region character varying(30),
    state_fips numeric(3,0),
    state_name character varying(30),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.city_state_zip OWNER TO clinlims;

--
-- Name: code_element_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.code_element_type (
    id numeric(10,0) NOT NULL,
    text character varying(60),
    lastupdated timestamp(6) without time zone,
    local_reference_table numeric(10,0)
);


ALTER TABLE clinlims.code_element_type OWNER TO clinlims;

--
-- Name: code_element_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.code_element_type_seq
    START WITH 21
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.code_element_type_seq OWNER TO clinlims;

--
-- Name: code_element_xref; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.code_element_xref (
    id numeric(10,0) NOT NULL,
    message_org_id numeric(10,0),
    code_element_type_id numeric(10,0),
    receiver_code_element_id numeric(10,0),
    local_code_element_id numeric(10,0),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.code_element_xref OWNER TO clinlims;

--
-- Name: code_element_xref_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.code_element_xref_seq
    START WITH 41
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.code_element_xref_seq OWNER TO clinlims;

--
-- Name: county_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.county_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.county_seq OWNER TO clinlims;

--
-- Name: data_indicator; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.data_indicator (
    id numeric(20,0) NOT NULL,
    data_value_id numeric(20,0),
    year integer,
    month integer,
    type_of_indicator_id numeric(20,0) NOT NULL,
    status character varying(10),
    lastupdated timestamp without time zone NOT NULL
);


ALTER TABLE clinlims.data_indicator OWNER TO clinlims;

--
-- Name: data_indicator_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.data_indicator_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.data_indicator_seq OWNER TO clinlims;

--
-- Name: data_resource; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.data_resource (
    id numeric(20,0) NOT NULL,
    name character varying(20),
    collection_name character varying(20),
    header_key character varying(40),
    level character varying(20),
    indicator_id numeric(20,0),
    lastupdated timestamp without time zone NOT NULL
);


ALTER TABLE clinlims.data_resource OWNER TO clinlims;

--
-- Name: data_resource_level_id; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.data_resource_level_id (
    id integer NOT NULL,
    level character varying(20),
    id_for_level character varying(20),
    data_resource_id numeric(20,0)
);


ALTER TABLE clinlims.data_resource_level_id OWNER TO clinlims;

--
-- Name: data_resource_level_id_id_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.data_resource_level_id_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.data_resource_level_id_id_seq OWNER TO clinlims;

--
-- Name: data_resource_level_id_id_seq; Type: SEQUENCE OWNED BY; Schema: clinlims; Owner: clinlims
--

ALTER SEQUENCE clinlims.data_resource_level_id_id_seq OWNED BY clinlims.data_resource_level_id.id;


--
-- Name: data_resource_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.data_resource_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.data_resource_seq OWNER TO clinlims;

--
-- Name: data_value; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.data_value (
    id numeric(20,0) NOT NULL,
    data_resource_id numeric(20,0),
    index numeric(20,0),
    column_name character varying(20),
    value character varying(20),
    display_key character varying(40),
    visible boolean,
    lastupdated timestamp without time zone NOT NULL
);


ALTER TABLE clinlims.data_value OWNER TO clinlims;

--
-- Name: data_value_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.data_value_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.data_value_seq OWNER TO clinlims;

--
-- Name: dictionary; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.dictionary (
    id numeric(10,0) NOT NULL,
    is_active character varying(1),
    dict_entry character varying(4000),
    lastupdated timestamp(6) without time zone,
    local_abbrev character varying(60),
    dictionary_category_id numeric(10,0),
    display_key character varying(60),
    sort_order numeric DEFAULT '0'::numeric,
    name_localization_id numeric
);


ALTER TABLE clinlims.dictionary OWNER TO clinlims;

--
-- Name: COLUMN dictionary.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.dictionary.is_active IS 'Flag indicating if the analyte is active';


--
-- Name: COLUMN dictionary.dict_entry; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.dictionary.dict_entry IS 'Finding, result, interpretation';


--
-- Name: COLUMN dictionary.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.dictionary.display_key IS 'Resource file lookup key for localization of displaying the name';


--
-- Name: COLUMN dictionary.sort_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.dictionary.sort_order IS 'Sets the sort order for dictionary categories.  The sort_order is WITHIN a category.';


--
-- Name: COLUMN dictionary.name_localization_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.dictionary.name_localization_id IS 'The id of the localization value';


--
-- Name: dictionary_category; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.dictionary_category (
    id numeric(10,0) NOT NULL,
    description character varying(60),
    lastupdated timestamp(6) without time zone,
    local_abbrev character varying(10),
    name character varying(50)
);


ALTER TABLE clinlims.dictionary_category OWNER TO clinlims;

--
-- Name: COLUMN dictionary_category.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.dictionary_category.id IS 'A unique auto generated integer number assigned by database';


--
-- Name: COLUMN dictionary_category.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.dictionary_category.description IS 'Human readable description';


--
-- Name: dictionary_category_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.dictionary_category_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.dictionary_category_seq OWNER TO clinlims;

--
-- Name: dictionary_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.dictionary_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.dictionary_seq OWNER TO clinlims;

--
-- Name: district_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.district_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.district_seq OWNER TO clinlims;

--
-- Name: document_track; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.document_track (
    id numeric(10,0) NOT NULL,
    table_id numeric(10,0) NOT NULL,
    row_id numeric(10,0) NOT NULL,
    document_type_id numeric(10,0) NOT NULL,
    parent_id numeric(10,0),
    report_generation_time timestamp with time zone,
    lastupdated timestamp with time zone,
    name character varying(80)
);


ALTER TABLE clinlims.document_track OWNER TO clinlims;

--
-- Name: TABLE document_track; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.document_track IS 'Table to track operations on documents.  Expected use is for has a document of some been printed for a sample, qa_event etc';


--
-- Name: COLUMN document_track.table_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_track.table_id IS 'The table to which the row_id references';


--
-- Name: COLUMN document_track.row_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_track.row_id IS 'The particular record for which a document has been generated';


--
-- Name: COLUMN document_track.document_type_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_track.document_type_id IS 'References the type of document which the record has been generated for';


--
-- Name: COLUMN document_track.parent_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_track.parent_id IS 'If the document has been generated more than once for this record then this will point to the previous record';


--
-- Name: COLUMN document_track.report_generation_time; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_track.report_generation_time IS 'When this report was generated';


--
-- Name: COLUMN document_track.lastupdated; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_track.lastupdated IS 'Last time this record was updated';


--
-- Name: COLUMN document_track.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_track.name IS 'The name of the report if there is more than one of the type';


--
-- Name: document_track_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.document_track_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.document_track_seq OWNER TO clinlims;

--
-- Name: document_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.document_type (
    id numeric(10,0) NOT NULL,
    name character varying(40) NOT NULL,
    description character varying(80),
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.document_type OWNER TO clinlims;

--
-- Name: TABLE document_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.document_type IS 'Table which describes document types to be tracked by document_track table';


--
-- Name: COLUMN document_type.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_type.name IS 'The name of the document';


--
-- Name: COLUMN document_type.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_type.description IS 'The description of the document';


--
-- Name: COLUMN document_type.lastupdated; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.document_type.lastupdated IS 'Last time this record was updated';


--
-- Name: document_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.document_type_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.document_type_seq OWNER TO clinlims;

--
-- Name: electronic_order; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.electronic_order (
    id numeric(10,0) NOT NULL,
    external_id character varying(60) NOT NULL,
    patient_id numeric(10,0),
    status_id numeric(10,0) NOT NULL,
    order_timestamp timestamp without time zone NOT NULL,
    data text NOT NULL,
    lastupdated timestamp without time zone NOT NULL
);


ALTER TABLE clinlims.electronic_order OWNER TO clinlims;

--
-- Name: COLUMN electronic_order.external_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.electronic_order.external_id IS 'An order ID assigned by another system (i.e. iSante) submitted electronically';


--
-- Name: COLUMN electronic_order.patient_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.electronic_order.patient_id IS 'The id and foreign key to the patient record for this electronic order';


--
-- Name: COLUMN electronic_order.status_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.electronic_order.status_id IS 'The ID and foreign key to the status_of_sample table. Possible status (Entered, Cancelled, Realized)';


--
-- Name: COLUMN electronic_order.order_timestamp; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.electronic_order.order_timestamp IS 'Timestamp when the order was received';


--
-- Name: COLUMN electronic_order.data; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.electronic_order.data IS 'The order in CLOB form';


--
-- Name: electronic_order_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.electronic_order_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.electronic_order_seq OWNER TO clinlims;

--
-- Name: gender; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.gender (
    id numeric(10,0) NOT NULL,
    gender_type character varying(1),
    description character varying(20),
    lastupdated timestamp(6) without time zone,
    name_key character varying(60)
);


ALTER TABLE clinlims.gender OWNER TO clinlims;

--
-- Name: COLUMN gender.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.gender.id IS 'A unique auto generated integer number assigned by database';


--
-- Name: COLUMN gender.gender_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.gender.gender_type IS 'Gender code (M, F, U)';


--
-- Name: COLUMN gender.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.gender.description IS 'Human readable description';


--
-- Name: gender_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.gender_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.gender_seq OWNER TO clinlims;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.hibernate_sequence OWNER TO clinlims;

--
-- Name: history; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.history (
    id numeric(10,0) NOT NULL,
    sys_user_id numeric(10,0) NOT NULL,
    reference_id numeric NOT NULL,
    reference_table numeric NOT NULL,
    "timestamp" timestamp without time zone NOT NULL,
    activity character varying(1) NOT NULL,
    changes bytea
);


ALTER TABLE clinlims.history OWNER TO clinlims;

--
-- Name: COLUMN history.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.history.id IS 'Sequential number for audit records';


--
-- Name: COLUMN history.sys_user_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.history.sys_user_id IS 'Sequential Identifier';


--
-- Name: COLUMN history.reference_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.history.reference_id IS 'Links to record in table to which this entry pertains';


--
-- Name: COLUMN history.reference_table; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.history.reference_table IS 'Link to table that this entity belongs to';


--
-- Name: COLUMN history."timestamp"; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.history."timestamp" IS 'Date of history record';


--
-- Name: COLUMN history.activity; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.history.activity IS 'U for update, D for delete';


--
-- Name: COLUMN history.changes; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.history.changes IS 'XML image of record prior to change';


--
-- Name: history_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.history_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.history_seq OWNER TO clinlims;

--
-- Name: hl7_encoding_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.hl7_encoding_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.hl7_encoding_type_seq OWNER TO clinlims;

--
-- Name: hl7_message_out; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.hl7_message_out (
    id numeric(20,0) NOT NULL,
    data text NOT NULL,
    lastupdated timestamp without time zone NOT NULL,
    status character varying(20) NOT NULL
);


ALTER TABLE clinlims.hl7_message_out OWNER TO clinlims;

--
-- Name: hl7_message_out_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.hl7_message_out_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.hl7_message_out_seq OWNER TO clinlims;

--
-- Name: htmldb_plan_table; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.htmldb_plan_table (
    statement_id character varying(30),
    plan_id numeric,
    "timestamp" timestamp without time zone,
    remarks character varying(4000),
    operation character varying(30),
    options character varying(255),
    object_node character varying(128),
    object_owner character varying(30),
    object_name character varying(30),
    object_alias character varying(65),
    object_instance numeric,
    object_type character varying(30),
    optimizer character varying(255),
    search_columns numeric,
    id numeric,
    parent_id numeric,
    depth numeric,
    "position" numeric,
    cost numeric,
    cardinality numeric,
    bytes numeric,
    other_tag character varying(255),
    partition_start character varying(255),
    partition_stop character varying(255),
    partition_id numeric,
    other text,
    distribution character varying(30),
    cpu_cost numeric,
    io_cost numeric,
    temp_space numeric,
    access_predicates character varying(4000),
    filter_predicates character varying(4000),
    projection character varying(4000),
    "time" numeric,
    qblock_name character varying(30)
);


ALTER TABLE clinlims.htmldb_plan_table OWNER TO clinlims;

--
-- Name: image; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.image (
    id numeric NOT NULL,
    description text,
    image bytea,
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.image OWNER TO clinlims;

--
-- Name: TABLE image; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.image IS 'Primary table for db based images';


--
-- Name: COLUMN image.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.image.id IS 'Primary key';


--
-- Name: COLUMN image.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.image.description IS 'Brief description of image';


--
-- Name: COLUMN image.image; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.image.image IS 'The image itself';


--
-- Name: image_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.image_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.image_seq OWNER TO clinlims;

--
-- Name: instrument; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.instrument (
    id numeric(10,0) NOT NULL,
    scrip_id numeric(10,0),
    name character varying(20),
    description character varying(60),
    instru_type character varying(30),
    is_active character varying(1),
    location character varying(60)
);


ALTER TABLE clinlims.instrument OWNER TO clinlims;

--
-- Name: COLUMN instrument.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument.name IS 'Short name for instrument';


--
-- Name: COLUMN instrument.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument.description IS 'Instrument description';


--
-- Name: COLUMN instrument.instru_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument.instru_type IS 'Type of instrument: Mass Spec, HPLC, etc.';


--
-- Name: COLUMN instrument.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument.is_active IS 'Flag indicating if the instrument is active';


--
-- Name: COLUMN instrument.location; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument.location IS 'Location of instrument';


--
-- Name: instrument_analyte; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.instrument_analyte (
    id numeric(10,0) NOT NULL,
    analyte_id numeric(10,0),
    instru_id numeric(10,0),
    method_id numeric(10,0),
    result_group numeric
);


ALTER TABLE clinlims.instrument_analyte OWNER TO clinlims;

--
-- Name: COLUMN instrument_analyte.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument_analyte.id IS 'Sequential Identifier';


--
-- Name: COLUMN instrument_analyte.method_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument_analyte.method_id IS 'Sequential number';


--
-- Name: COLUMN instrument_analyte.result_group; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument_analyte.result_group IS 'A program generated group number';


--
-- Name: instrument_log; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.instrument_log (
    id numeric(10,0) NOT NULL,
    instru_id numeric(10,0),
    instlog_type character varying(1),
    event_begin timestamp without time zone,
    event_end timestamp without time zone
);


ALTER TABLE clinlims.instrument_log OWNER TO clinlims;

--
-- Name: COLUMN instrument_log.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument_log.id IS 'Sequential Identifier';


--
-- Name: COLUMN instrument_log.instlog_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument_log.instlog_type IS 'type of log entry: downtime, maintenance';


--
-- Name: COLUMN instrument_log.event_begin; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument_log.event_begin IS 'date-time logged event started';


--
-- Name: COLUMN instrument_log.event_end; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.instrument_log.event_end IS 'Date-time logged event ended';


--
-- Name: inventory_component; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.inventory_component (
    id numeric(10,0) NOT NULL,
    invitem_id numeric(10,0),
    quantity numeric,
    material_component_id numeric(10,0)
);


ALTER TABLE clinlims.inventory_component OWNER TO clinlims;

--
-- Name: COLUMN inventory_component.quantity; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_component.quantity IS 'Quantity of material required';


--
-- Name: inventory_item; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.inventory_item (
    id numeric(10,0) NOT NULL,
    uom_id numeric(10,0),
    name character varying(20),
    description character varying(60),
    quantity_min_level numeric,
    quantity_max_level numeric,
    quantity_to_reorder numeric,
    is_reorder_auto character varying(1),
    is_lot_maintained character varying(1),
    is_active character varying(1),
    average_lead_time numeric,
    average_cost numeric,
    average_daily_use numeric
);


ALTER TABLE clinlims.inventory_item OWNER TO clinlims;

--
-- Name: COLUMN inventory_item.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.name IS 'Unique Short Name for this item ie "Red Top Tube"';


--
-- Name: COLUMN inventory_item.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.description IS 'Description of Item';


--
-- Name: COLUMN inventory_item.quantity_min_level; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.quantity_min_level IS 'Minimum inventory level';


--
-- Name: COLUMN inventory_item.quantity_max_level; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.quantity_max_level IS 'Maximum Inventory Level';


--
-- Name: COLUMN inventory_item.quantity_to_reorder; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.quantity_to_reorder IS 'Amount to reorder';


--
-- Name: COLUMN inventory_item.is_reorder_auto; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.is_reorder_auto IS 'Flag indicating system should automatically reorder';


--
-- Name: COLUMN inventory_item.is_lot_maintained; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.is_lot_maintained IS 'Indicates if individual lot information is maintained for this item';


--
-- Name: COLUMN inventory_item.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.is_active IS 'Indicates if this item is available for use.';


--
-- Name: COLUMN inventory_item.average_lead_time; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.average_lead_time IS 'Average lead time in days';


--
-- Name: COLUMN inventory_item.average_cost; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.average_cost IS 'Average cost per unit';


--
-- Name: COLUMN inventory_item.average_daily_use; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_item.average_daily_use IS 'Seasonally adjusted average usage per day';


--
-- Name: inventory_item_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.inventory_item_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.inventory_item_seq OWNER TO clinlims;

--
-- Name: inventory_location; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.inventory_location (
    id numeric(10,0) NOT NULL,
    storage_id numeric(10,0),
    lot_number character varying(20),
    quantity_onhand numeric,
    expiration_date timestamp without time zone,
    inv_item_id numeric(10,0)
);


ALTER TABLE clinlims.inventory_location OWNER TO clinlims;

--
-- Name: COLUMN inventory_location.lot_number; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_location.lot_number IS 'Lot number';


--
-- Name: COLUMN inventory_location.quantity_onhand; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_location.quantity_onhand IS 'Number of units onhand';


--
-- Name: inventory_location_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.inventory_location_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.inventory_location_seq OWNER TO clinlims;

--
-- Name: inventory_receipt; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.inventory_receipt (
    id numeric(10,0) NOT NULL,
    invitem_id numeric(10,0) NOT NULL,
    received_date timestamp without time zone,
    quantity_received numeric,
    unit_cost numeric,
    qc_reference character varying(20),
    external_reference character varying(20),
    org_id numeric(10,0)
);


ALTER TABLE clinlims.inventory_receipt OWNER TO clinlims;

--
-- Name: COLUMN inventory_receipt.received_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_receipt.received_date IS 'Date and time item was received';


--
-- Name: COLUMN inventory_receipt.quantity_received; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_receipt.quantity_received IS 'Number of units received';


--
-- Name: COLUMN inventory_receipt.unit_cost; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_receipt.unit_cost IS 'Cost per unit item';


--
-- Name: COLUMN inventory_receipt.qc_reference; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_receipt.qc_reference IS 'External QC reference number';


--
-- Name: COLUMN inventory_receipt.external_reference; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.inventory_receipt.external_reference IS 'External reference to purchase order, invoice number.';


--
-- Name: inventory_receipt_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.inventory_receipt_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.inventory_receipt_seq OWNER TO clinlims;

--
-- Name: label; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.label (
    id numeric(10,0) NOT NULL,
    name character varying(30),
    description character varying(60),
    printer_type character(1),
    scriptlet_id numeric(10,0),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.label OWNER TO clinlims;

--
-- Name: label_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.label_seq
    START WITH 3
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.label_seq OWNER TO clinlims;

--
-- Name: localization; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.localization (
    id numeric NOT NULL,
    description text,
    english text NOT NULL,
    french text NOT NULL,
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.localization OWNER TO clinlims;

--
-- Name: TABLE localization; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.localization IS 'This is for localization maintained by end user';


--
-- Name: COLUMN localization.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.localization.description IS 'A brief description to give some context';


--
-- Name: COLUMN localization.english; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.localization.english IS 'English localization';


--
-- Name: COLUMN localization.french; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.localization.french IS 'French localization';


--
-- Name: localization_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.localization_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.localization_seq OWNER TO clinlims;

--
-- Name: login_user; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.login_user (
    id numeric(10,0) NOT NULL,
    login_name character varying(20) NOT NULL,
    password character varying(256) NOT NULL,
    password_expired_dt date NOT NULL,
    account_locked character varying(1) NOT NULL,
    account_disabled character varying(1) NOT NULL,
    is_admin character varying(1) NOT NULL,
    user_time_out character varying(3) NOT NULL
);


ALTER TABLE clinlims.login_user OWNER TO clinlims;

--
-- Name: COLUMN login_user.login_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.login_user.login_name IS 'User LOGIN_NAME from SYSTEM_USER table';


--
-- Name: COLUMN login_user.password; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.login_user.password IS 'User password';


--
-- Name: COLUMN login_user.password_expired_dt; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.login_user.password_expired_dt IS 'Password expiration date';


--
-- Name: COLUMN login_user.account_locked; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.login_user.account_locked IS 'Account locked (Y/N)';


--
-- Name: COLUMN login_user.account_disabled; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.login_user.account_disabled IS 'Account disabled (Y/N)';


--
-- Name: COLUMN login_user.is_admin; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.login_user.is_admin IS 'Indicates if this user is administrator (Y/N)';


--
-- Name: COLUMN login_user.user_time_out; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.login_user.user_time_out IS 'User session time out in minute';


--
-- Name: login_user_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.login_user_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.login_user_seq OWNER TO clinlims;

--
-- Name: menu; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.menu (
    id numeric(10,0) NOT NULL,
    parent_id numeric(10,0),
    presentation_order numeric,
    element_id character varying NOT NULL,
    action_url character varying(120),
    click_action character varying(120),
    display_key character varying(60),
    tool_tip_key character varying(60),
    new_window boolean DEFAULT false,
    is_active boolean DEFAULT true
);


ALTER TABLE clinlims.menu OWNER TO clinlims;

--
-- Name: TABLE menu; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.menu IS 'Table for menuing system';


--
-- Name: COLUMN menu.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.id IS 'primary key';


--
-- Name: COLUMN menu.parent_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.parent_id IS 'If this is a submenu then the parent menu id';


--
-- Name: COLUMN menu.presentation_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.presentation_order IS 'For top level menus the order across the page for sub menu the order in the popup menu.  If there is a collision then the order is alphebetical';


--
-- Name: COLUMN menu.element_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.element_id IS 'The element id in the context of HTML.';


--
-- Name: COLUMN menu.action_url; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.action_url IS 'If clicking on the element moves to a new page, the url of that page';


--
-- Name: COLUMN menu.click_action; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.click_action IS 'If clicking on the element calls javascript then the javascript call';


--
-- Name: COLUMN menu.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.display_key IS 'The message key for what will be displayed in the menu';


--
-- Name: COLUMN menu.tool_tip_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.tool_tip_key IS 'The message key for the tool-tip';


--
-- Name: COLUMN menu.new_window; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.new_window IS 'If true the menu action will be done in a new window';


--
-- Name: COLUMN menu.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.menu.is_active IS 'Is this menu item active.  Allows menu to be turned on/off programatically';


--
-- Name: menu_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.menu_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.menu_seq OWNER TO clinlims;

--
-- Name: message_org; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.message_org (
    id numeric(10,0) NOT NULL,
    org_id character varying(60),
    is_active character varying(1),
    active_begin timestamp without time zone,
    active_end timestamp without time zone,
    description character varying(60),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.message_org OWNER TO clinlims;

--
-- Name: message_org_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.message_org_seq
    START WITH 41
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.message_org_seq OWNER TO clinlims;

--
-- Name: messagecontrolidseq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.messagecontrolidseq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.messagecontrolidseq OWNER TO clinlims;

--
-- Name: method; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.method (
    id numeric(10,0) NOT NULL,
    name character varying(20) NOT NULL,
    description character varying(60) NOT NULL,
    reporting_description character varying(60),
    is_active character varying(1),
    active_begin timestamp without time zone,
    active_end timestamp without time zone,
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.method OWNER TO clinlims;

--
-- Name: COLUMN method.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method.id IS 'Sequential number';


--
-- Name: COLUMN method.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method.name IS 'Sort name for method';


--
-- Name: COLUMN method.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method.description IS 'Description for Method';


--
-- Name: COLUMN method.reporting_description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method.reporting_description IS 'Description that appears on reports';


--
-- Name: COLUMN method.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method.is_active IS 'Flag indicating if the test is active';


--
-- Name: COLUMN method.active_begin; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method.active_begin IS 'Date test was moved into production';


--
-- Name: COLUMN method.active_end; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method.active_end IS 'Date test was removed from production';


--
-- Name: method_analyte; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.method_analyte (
    id numeric(10,0) NOT NULL,
    method_id numeric(10,0),
    analyte_id numeric(10,0),
    result_group numeric,
    sort_order numeric,
    ma_type character varying(1)
);


ALTER TABLE clinlims.method_analyte OWNER TO clinlims;

--
-- Name: COLUMN method_analyte.method_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_analyte.method_id IS 'Sequential number';


--
-- Name: COLUMN method_analyte.result_group; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_analyte.result_group IS 'a program generated group (sequence) number';


--
-- Name: COLUMN method_analyte.sort_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_analyte.sort_order IS 'The order the analytes are displayed (sort order)';


--
-- Name: COLUMN method_analyte.ma_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_analyte.ma_type IS 'type of analyte';


--
-- Name: method_result; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.method_result (
    id numeric(10,0) NOT NULL,
    scrip_id numeric(10,0),
    result_group numeric,
    flags character varying(10),
    methres_type character varying(1),
    value character varying(80),
    quant_limit character varying(30),
    cont_level character varying(30),
    method_id numeric(10,0)
);


ALTER TABLE clinlims.method_result OWNER TO clinlims;

--
-- Name: COLUMN method_result.result_group; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_result.result_group IS 'The method analyte result group number';


--
-- Name: COLUMN method_result.flags; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_result.flags IS 'A string of 1 character codes: Positive, Reportable';


--
-- Name: COLUMN method_result.methres_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_result.methres_type IS 'Type of parameter: Dictionary, Titer-range, Number-range, date';


--
-- Name: COLUMN method_result.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_result.value IS 'A possible result value based on type';


--
-- Name: COLUMN method_result.quant_limit; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_result.quant_limit IS 'Quantitation Limit (if any)';


--
-- Name: COLUMN method_result.cont_level; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.method_result.cont_level IS 'Contaminant level (if any)';


--
-- Name: method_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.method_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.method_seq OWNER TO clinlims;

--
-- Name: mls_lab_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.mls_lab_type (
    id numeric(10,0) NOT NULL,
    description character varying(50) NOT NULL,
    org_mlt_org_mlt_id numeric(10,0)
);


ALTER TABLE clinlims.mls_lab_type OWNER TO clinlims;

--
-- Name: COLUMN mls_lab_type.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.mls_lab_type.id IS 'Sequential ID';


--
-- Name: COLUMN mls_lab_type.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.mls_lab_type.description IS 'Used to define MLS lab types including Level A, Urine, Virology';


--
-- Name: nc_event; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.nc_event (
    id integer NOT NULL,
    name character varying(200),
    name_of_reporter character varying(200),
    report_date date,
    nce_number character varying(200),
    date_of_event date,
    lab_order_number character varying(30),
    prescriber_name character varying(200),
    site character varying(200),
    reporting_unit_id integer,
    description text,
    suspected_causes text,
    proposed_action text,
    laboratory_component character varying(10),
    nce_category_id integer,
    consequence_id character varying(10),
    recurrence_id character varying(10),
    severity_score character varying(5),
    color_code character varying(10),
    corrective_action text,
    control_action text,
    comments text,
    effective character varying(5),
    signature character varying(200),
    date_completed date,
    nce_type_id integer,
    status character varying(100),
    discussion_date character varying(100)
);


ALTER TABLE clinlims.nc_event OWNER TO clinlims;

--
-- Name: nc_event_id_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.nc_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE clinlims.nc_event_id_seq OWNER TO clinlims;

--
-- Name: nce_action_log; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.nce_action_log (
    id integer NOT NULL,
    corrective_action text,
    action_type character varying(100),
    date_completed date,
    turn_around_time integer,
    nce_event_id integer,
    person_responsible character varying(200)
);


ALTER TABLE clinlims.nce_action_log OWNER TO clinlims;

--
-- Name: nce_action_log_id_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.nce_action_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE clinlims.nce_action_log_id_seq OWNER TO clinlims;

--
-- Name: nce_category; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.nce_category (
    id integer NOT NULL,
    name character varying(200),
    display_key character varying(100),
    active boolean DEFAULT true,
    last_updated timestamp with time zone
);


ALTER TABLE clinlims.nce_category OWNER TO clinlims;

--
-- Name: nce_category_id_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.nce_category_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE clinlims.nce_category_id_seq OWNER TO clinlims;

--
-- Name: nce_specimen; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.nce_specimen (
    id integer NOT NULL,
    nce_id integer,
    sample_item_id integer
);


ALTER TABLE clinlims.nce_specimen OWNER TO clinlims;

--
-- Name: nce_specimen_id_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.nce_specimen_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE clinlims.nce_specimen_id_seq OWNER TO clinlims;

--
-- Name: nce_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.nce_type (
    id integer NOT NULL,
    name character varying(200),
    display_key character varying(100),
    category_id integer,
    active boolean DEFAULT true,
    last_updated timestamp with time zone
);


ALTER TABLE clinlims.nce_type OWNER TO clinlims;

--
-- Name: nce_type_id_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.nce_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE clinlims.nce_type_id_seq OWNER TO clinlims;

--
-- Name: note; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.note (
    id numeric(10,0) NOT NULL,
    sys_user_id numeric(10,0),
    reference_id numeric,
    reference_table numeric,
    note_type character varying(1),
    subject character varying(60),
    text character varying(1000),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.note OWNER TO clinlims;

--
-- Name: COLUMN note.sys_user_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.note.sys_user_id IS 'Sequential Identifier';


--
-- Name: COLUMN note.reference_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.note.reference_id IS 'Link to record in table to which this entry pertains.';


--
-- Name: COLUMN note.reference_table; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.note.reference_table IS 'Link to table that this entity belongs to';


--
-- Name: COLUMN note.note_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.note.note_type IS 'Type of comment such as external, internal';


--
-- Name: COLUMN note.subject; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.note.subject IS 'Comment subject';


--
-- Name: note_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.note_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.note_seq OWNER TO clinlims;

--
-- Name: observation_history; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.observation_history (
    id numeric(10,0) NOT NULL,
    patient_id numeric(10,0) NOT NULL,
    sample_id numeric(10,0) NOT NULL,
    observation_history_type_id numeric(10,0) NOT NULL,
    value_type character varying(5) NOT NULL,
    value character varying(80),
    lastupdated timestamp with time zone,
    sample_item_id numeric(10,0)
);


ALTER TABLE clinlims.observation_history OWNER TO clinlims;

--
-- Name: TABLE observation_history; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.observation_history IS 'defines the answer at the time of a interview (with sample) to a demographic question.';


--
-- Name: COLUMN observation_history.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history.id IS 'primary key';


--
-- Name: COLUMN observation_history.patient_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history.patient_id IS 'FK linking this information to a patient';


--
-- Name: COLUMN observation_history.sample_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history.sample_id IS 'FK linking this information to a sample (and a collection date).';


--
-- Name: COLUMN observation_history.observation_history_type_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history.observation_history_type_id IS 'FK to type table to define what is contained in the value column';


--
-- Name: COLUMN observation_history.value_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history.value_type IS 'L=local or literal, a value right here in the history table. D=Defined/Dictionary, the value is a foreign key to the Dictionary table. For multiple choice questions with fixed answers.';


--
-- Name: COLUMN observation_history.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history.value IS 'either a literal value for this demo. type, or a Foreign key to dictionary';


--
-- Name: COLUMN observation_history.sample_item_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history.sample_item_id IS 'Optional refereence to sample item';


--
-- Name: observation_history_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.observation_history_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.observation_history_seq OWNER TO clinlims;

--
-- Name: observation_history_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.observation_history_type (
    id numeric(10,0) NOT NULL,
    type_name character varying(30) NOT NULL,
    description character varying(400),
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.observation_history_type OWNER TO clinlims;

--
-- Name: TABLE observation_history_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.observation_history_type IS 'defines the possible classes of values allowed in the demo. history table.';


--
-- Name: COLUMN observation_history_type.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history_type.id IS 'primary key';


--
-- Name: COLUMN observation_history_type.type_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history_type.type_name IS 'a short tag (1 word) for this type. What was asked "Gender", "HIVPositive" etc.';


--
-- Name: COLUMN observation_history_type.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history_type.description IS 'a long description of this type.';


--
-- Name: COLUMN observation_history_type.lastupdated; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.observation_history_type.lastupdated IS 'the last time this item was written to the database.';


--
-- Name: observation_history_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.observation_history_type_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.observation_history_type_seq OWNER TO clinlims;

--
-- Name: occupation_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.occupation_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.occupation_seq OWNER TO clinlims;

--
-- Name: or_properties; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.or_properties (
    property_id integer NOT NULL,
    property_key character varying(255) NOT NULL,
    property_value character varying(255)
);


ALTER TABLE clinlims.or_properties OWNER TO clinlims;

--
-- Name: or_tags; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.or_tags (
    tag_id integer NOT NULL,
    tagged_object_id integer NOT NULL,
    tagged_object_class character varying(255) NOT NULL,
    tag_value character varying(255) NOT NULL,
    tag_type character varying(255) NOT NULL
);


ALTER TABLE clinlims.or_tags OWNER TO clinlims;

--
-- Name: order_item; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.order_item (
    id numeric(10,0) NOT NULL,
    ord_id numeric(10,0) NOT NULL,
    quantity_requested numeric,
    quantity_received numeric,
    inv_loc_id numeric(10,0)
);


ALTER TABLE clinlims.order_item OWNER TO clinlims;

--
-- Name: COLUMN order_item.quantity_requested; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.order_item.quantity_requested IS 'Quantity requested by organization';


--
-- Name: COLUMN order_item.quantity_received; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.order_item.quantity_received IS 'Quantity received';


--
-- Name: orders; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.orders (
    id numeric(10,0) NOT NULL,
    org_id numeric(10,0) NOT NULL,
    sys_user_id numeric(10,0),
    ordered_date timestamp without time zone,
    neededby_date timestamp without time zone,
    requested_by character varying(30),
    cost_center character varying(15),
    shipping_type character varying(2),
    shipping_carrier character varying(2),
    shipping_cost numeric,
    delivered_date timestamp without time zone,
    is_external character varying(1),
    external_order_number character varying(20),
    is_filled character varying(1)
);


ALTER TABLE clinlims.orders OWNER TO clinlims;

--
-- Name: COLUMN orders.org_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.org_id IS 'Sequential Numbering Field';


--
-- Name: COLUMN orders.sys_user_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.sys_user_id IS 'Sequential Identifier';


--
-- Name: COLUMN orders.neededby_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.neededby_date IS 'Date-time the order must be received by client';


--
-- Name: COLUMN orders.requested_by; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.requested_by IS 'Name of person/entity who placed the order';


--
-- Name: COLUMN orders.cost_center; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.cost_center IS 'Entity or project the order will be charged against';


--
-- Name: COLUMN orders.shipping_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.shipping_type IS 'Type of shipping such as: Air, ground, First Class, Bulk....';


--
-- Name: COLUMN orders.shipping_carrier; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.shipping_carrier IS 'Company used for shipping: UPS, FedEx, USPS...';


--
-- Name: COLUMN orders.shipping_cost; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.shipping_cost IS 'Shipping Cost';


--
-- Name: COLUMN orders.delivered_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.delivered_date IS 'Date-time shipment received by client (including us)';


--
-- Name: COLUMN orders.is_external; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.is_external IS 'Indicates if current order will be filled by an external vendor';


--
-- Name: COLUMN orders.external_order_number; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.external_order_number IS 'External order number';


--
-- Name: COLUMN orders.is_filled; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.orders.is_filled IS 'Flag indicating if the order has been filled';


--
-- Name: org_hl7_encoding_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.org_hl7_encoding_type (
    organization_id numeric(10,0) NOT NULL,
    encoding_type_id numeric(10,0) NOT NULL,
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.org_hl7_encoding_type OWNER TO clinlims;

--
-- Name: TABLE org_hl7_encoding_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.org_hl7_encoding_type IS 'mapping table to identify which organization uses which hly encoding schema';


--
-- Name: org_mls_lab_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.org_mls_lab_type (
    org_id numeric NOT NULL,
    mls_lab_id numeric NOT NULL,
    org_mlt_id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.org_mls_lab_type OWNER TO clinlims;

--
-- Name: COLUMN org_mls_lab_type.org_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.org_mls_lab_type.org_id IS 'foreign key from organization table';


--
-- Name: COLUMN org_mls_lab_type.mls_lab_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.org_mls_lab_type.mls_lab_id IS 'foreign key from MLS lab type table';


--
-- Name: organization; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.organization (
    id numeric(10,0) NOT NULL,
    name character varying(80) NOT NULL,
    city character varying(30),
    zip_code character varying(10),
    mls_sentinel_lab_flag character varying(1) DEFAULT 'N'::character varying NOT NULL,
    org_mlt_org_mlt_id numeric(10,0),
    org_id numeric(10,0),
    short_name character varying(15) DEFAULT '""'::character varying,
    multiple_unit character varying(30),
    street_address character varying(30),
    state character varying(2),
    internet_address character varying(40),
    clia_num character varying(12),
    pws_id character varying(15),
    lastupdated timestamp(6) without time zone,
    mls_lab_flag character(1),
    is_active character(1),
    local_abbrev character varying(10),
    code character varying(20),
    datim_org_code character varying(25),
    datim_org_name character varying(100)
);


ALTER TABLE clinlims.organization OWNER TO clinlims;

--
-- Name: COLUMN organization.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.id IS 'Sequential Numbering Field';


--
-- Name: COLUMN organization.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.name IS 'The full name of an organization';


--
-- Name: COLUMN organization.city; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.city IS 'City';


--
-- Name: COLUMN organization.zip_code; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.zip_code IS 'Zip code';


--
-- Name: COLUMN organization.mls_sentinel_lab_flag; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.mls_sentinel_lab_flag IS 'Yes or No field indicating that the submitter is an Minnesota Laboratory System Lab';


--
-- Name: COLUMN organization.org_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.org_id IS 'Sequential Numbering Field';


--
-- Name: COLUMN organization.short_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.short_name IS 'A shortened or abbreviated name of an organization. For example "BCBSM" is the short name for Blue Cross Blue Shield Minnesota';


--
-- Name: COLUMN organization.multiple_unit; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.multiple_unit IS 'Apartment number or unit information';


--
-- Name: COLUMN organization.street_address; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.street_address IS 'Street address for this organization';


--
-- Name: COLUMN organization.state; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.state IS 'State or Province';


--
-- Name: COLUMN organization.internet_address; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.internet_address IS 'A uniform resource locator (URL), ie a website address assigned to an entity or pager.';


--
-- Name: COLUMN organization.clia_num; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.clia_num IS 'Clinical Laboratory Improvement Amendments number';


--
-- Name: COLUMN organization.pws_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.pws_id IS 'Public water supply id (SDWIS)';


--
-- Name: COLUMN organization.code; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization.code IS 'The code for this organization';


--
-- Name: organization_address; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.organization_address (
    organization_id numeric(10,0) NOT NULL,
    address_part_id numeric(10,0) NOT NULL,
    type character(1) DEFAULT 'T'::bpchar,
    value character varying(120)
);


ALTER TABLE clinlims.organization_address OWNER TO clinlims;

--
-- Name: TABLE organization_address; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.organization_address IS 'Join table between address parts and an organization';


--
-- Name: COLUMN organization_address.organization_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_address.organization_id IS 'The id of the organization who this address is attached to';


--
-- Name: COLUMN organization_address.address_part_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_address.address_part_id IS 'The id of the address part for which we have a value';


--
-- Name: COLUMN organization_address.type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_address.type IS 'The type of the value, either D for dictionary or T for text';


--
-- Name: COLUMN organization_address.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_address.value IS 'The actual value for this part of the address';


--
-- Name: organization_contact; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.organization_contact (
    id numeric(10,0) NOT NULL,
    organization_id numeric(10,0) NOT NULL,
    person_id numeric(10,0) NOT NULL,
    "position" character varying(32)
);


ALTER TABLE clinlims.organization_contact OWNER TO clinlims;

--
-- Name: TABLE organization_contact; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.organization_contact IS 'A join table between organizations and persons';


--
-- Name: COLUMN organization_contact.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_contact.id IS 'Unique key for each row';


--
-- Name: COLUMN organization_contact.organization_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_contact.organization_id IS 'The id of the organization being referred to';


--
-- Name: COLUMN organization_contact.person_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_contact.person_id IS 'The id of the person being referred to';


--
-- Name: COLUMN organization_contact."position"; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_contact."position" IS 'The position of the person within the organization';


--
-- Name: organization_contact_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.organization_contact_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.organization_contact_seq OWNER TO clinlims;

--
-- Name: organization_organization_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.organization_organization_type (
    org_id numeric(10,0) NOT NULL,
    org_type_id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.organization_organization_type OWNER TO clinlims;

--
-- Name: TABLE organization_organization_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.organization_organization_type IS 'many to many mapping table between organization and orginaztion type';


--
-- Name: organization_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.organization_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.organization_seq OWNER TO clinlims;

--
-- Name: organization_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.organization_type (
    id numeric(10,0) NOT NULL,
    short_name character varying(20) NOT NULL,
    description character varying(60),
    name_display_key character varying(60),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.organization_type OWNER TO clinlims;

--
-- Name: TABLE organization_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.organization_type IS 'The type of an organization.  Releationship will be many to many';


--
-- Name: COLUMN organization_type.short_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_type.short_name IS 'The name to be displayed in when organization type is to be associated with an organization';


--
-- Name: COLUMN organization_type.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_type.description IS 'Optional longer description of the type';


--
-- Name: COLUMN organization_type.name_display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.organization_type.name_display_key IS 'Resource file lookup key for localization of displaying the name';


--
-- Name: organization_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.organization_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.organization_type_seq OWNER TO clinlims;

--
-- Name: package_1; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.package_1 (
    id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.package_1 OWNER TO clinlims;

--
-- Name: panel; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.panel (
    id numeric(10,0) NOT NULL,
    name character varying,
    description character varying NOT NULL,
    lastupdated timestamp(6) without time zone,
    sort_order numeric DEFAULT 2147483647,
    is_active character varying(1) DEFAULT 'Y'::character varying,
    name_localization_id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.panel OWNER TO clinlims;

--
-- Name: COLUMN panel.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.panel.is_active IS 'Is this panel currently active';


--
-- Name: COLUMN panel.name_localization_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.panel.name_localization_id IS 'reference to the localization record for the panel name';


--
-- Name: panel_item; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.panel_item (
    id numeric(10,0) NOT NULL,
    panel_id numeric(10,0) NOT NULL,
    sort_order numeric,
    test_local_abbrev character varying(20),
    method_name character varying(20),
    lastupdated timestamp(6) without time zone,
    test_name character varying(20),
    test_id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.panel_item OWNER TO clinlims;

--
-- Name: COLUMN panel_item.sort_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.panel_item.sort_order IS 'The order the tests are displayed (sort order)';


--
-- Name: COLUMN panel_item.test_local_abbrev; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.panel_item.test_local_abbrev IS 'Short test name';


--
-- Name: COLUMN panel_item.method_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.panel_item.method_name IS 'Short method name';


--
-- Name: panel_item_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.panel_item_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.panel_item_seq OWNER TO clinlims;

--
-- Name: panel_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.panel_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.panel_seq OWNER TO clinlims;

--
-- Name: patient; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.patient (
    id numeric(10,0) NOT NULL,
    person_id numeric(10,0) NOT NULL,
    race character varying(5),
    gender character varying(1),
    birth_date timestamp without time zone,
    epi_first_name character varying(25),
    epi_middle_name character varying(25),
    epi_last_name character varying(240),
    birth_time timestamp without time zone,
    death_date timestamp without time zone,
    national_id character varying(255),
    ethnicity character varying(1),
    school_attend character varying(240),
    medicare_id character varying(240),
    medicaid_id character varying(240),
    birth_place character varying(255),
    lastupdated timestamp(6) without time zone,
    external_id character varying(255),
    chart_number character varying(20),
    entered_birth_date character varying(10)
);


ALTER TABLE clinlims.patient OWNER TO clinlims;

--
-- Name: COLUMN patient.race; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.race IS 'A string of 1 character race code(s)';


--
-- Name: COLUMN patient.gender; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.gender IS 'A one character gender code';


--
-- Name: COLUMN patient.birth_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.birth_date IS 'Date of birth';


--
-- Name: COLUMN patient.birth_time; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.birth_time IS 'Time of birth for newborn patients';


--
-- Name: COLUMN patient.death_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.death_date IS 'Date of death if unexplained illness or death';


--
-- Name: COLUMN patient.national_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.national_id IS 'National Patient ID or SSN';


--
-- Name: COLUMN patient.medicare_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.medicare_id IS 'Medicare Number';


--
-- Name: COLUMN patient.medicaid_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.medicaid_id IS 'Medicaid Number';


--
-- Name: COLUMN patient.entered_birth_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient.entered_birth_date IS 'Persons birthdate may not be known and it will be entered with XX for date and/or month';


--
-- Name: patient_identity; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.patient_identity (
    id numeric(10,0) NOT NULL,
    identity_type_id numeric(10,0),
    patient_id numeric(10,0),
    identity_data character varying(255),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.patient_identity OWNER TO clinlims;

--
-- Name: patient_identity_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.patient_identity_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.patient_identity_seq OWNER TO clinlims;

--
-- Name: patient_identity_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.patient_identity_type (
    id numeric(10,0) NOT NULL,
    identity_type character varying(30),
    description character varying(400),
    lastupdated timestamp without time zone
);


ALTER TABLE clinlims.patient_identity_type OWNER TO clinlims;

--
-- Name: patient_identity_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.patient_identity_type_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.patient_identity_type_seq OWNER TO clinlims;

--
-- Name: patient_occupation_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.patient_occupation_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.patient_occupation_seq OWNER TO clinlims;

--
-- Name: patient_patient_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.patient_patient_type (
    id numeric(10,0) NOT NULL,
    patient_type_id numeric(10,0),
    patient_id numeric(10,0),
    lastupdated timestamp without time zone
);


ALTER TABLE clinlims.patient_patient_type OWNER TO clinlims;

--
-- Name: patient_patient_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.patient_patient_type_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.patient_patient_type_seq OWNER TO clinlims;

--
-- Name: patient_relation_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.patient_relation_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.patient_relation_seq OWNER TO clinlims;

--
-- Name: patient_relations; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.patient_relations (
    id numeric(10,0) NOT NULL,
    pat_id_source numeric(10,0) NOT NULL,
    pat_id numeric(10,0),
    relation character varying(1)
);


ALTER TABLE clinlims.patient_relations OWNER TO clinlims;

--
-- Name: COLUMN patient_relations.relation; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.patient_relations.relation IS 'Type of relation (mother to child, parent to child, sibling)';


--
-- Name: patient_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.patient_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.patient_seq OWNER TO clinlims;

--
-- Name: patient_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.patient_type (
    id numeric(10,0) NOT NULL,
    type character varying(300),
    description character varying(4000),
    lastupdated timestamp without time zone
);


ALTER TABLE clinlims.patient_type OWNER TO clinlims;

--
-- Name: patient_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.patient_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.patient_type_seq OWNER TO clinlims;

--
-- Name: payment_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.payment_type (
    id numeric(10,0) NOT NULL,
    type character varying(300),
    description character varying(4000)
);


ALTER TABLE clinlims.payment_type OWNER TO clinlims;

--
-- Name: payment_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.payment_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.payment_type_seq OWNER TO clinlims;

--
-- Name: person; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.person (
    id numeric(10,0) NOT NULL,
    last_name character varying(255),
    first_name character varying(255),
    middle_name character varying(255),
    multiple_unit character varying(255),
    street_address character varying(255),
    city character varying(255),
    state character(2),
    zip_code character(10),
    country character varying(255),
    work_phone character varying(255),
    home_phone character varying(255),
    cell_phone character varying(255),
    fax character varying(255),
    email character varying(255),
    lastupdated timestamp(6) without time zone,
    primary_phone character varying(255)
);


ALTER TABLE clinlims.person OWNER TO clinlims;

--
-- Name: COLUMN person.last_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.person.last_name IS 'Last name';


--
-- Name: COLUMN person.first_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.person.first_name IS 'Person Name';


--
-- Name: COLUMN person.middle_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.person.middle_name IS 'Middle Name';


--
-- Name: COLUMN person.multiple_unit; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.person.multiple_unit IS 'Designates a specific part of a building, such as "APT 212"';


--
-- Name: person_address; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.person_address (
    person_id numeric(10,0) NOT NULL,
    address_part_id numeric(10,0) NOT NULL,
    type character(1) DEFAULT 'T'::bpchar,
    value character varying(255)
);


ALTER TABLE clinlims.person_address OWNER TO clinlims;

--
-- Name: TABLE person_address; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.person_address IS 'Join table between address parts and a person';


--
-- Name: COLUMN person_address.person_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.person_address.person_id IS 'The id of the person who this address is attached to';


--
-- Name: COLUMN person_address.address_part_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.person_address.address_part_id IS 'The id of the address part for which we have a value';


--
-- Name: COLUMN person_address.type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.person_address.type IS 'The type of the value, either D for dictionary or T for text';


--
-- Name: COLUMN person_address.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.person_address.value IS 'The actual value for this part of the address';


--
-- Name: person_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.person_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.person_seq OWNER TO clinlims;

--
-- Name: program; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.program (
    id numeric(10,0) NOT NULL,
    code character varying(10) NOT NULL,
    name character varying(50) NOT NULL,
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.program OWNER TO clinlims;

--
-- Name: COLUMN program.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.program.name IS 'EPI PROGRAM NAME';


--
-- Name: program_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.program_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.program_seq OWNER TO clinlims;

--
-- Name: project; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.project (
    id numeric(10,0) NOT NULL,
    name character varying(50) NOT NULL,
    sys_user_id numeric(10,0),
    description character varying(60),
    started_date timestamp without time zone,
    completed_date timestamp without time zone,
    is_active character varying(1),
    reference_to character varying(20),
    program_code character varying(10),
    lastupdated timestamp(6) without time zone,
    scriptlet_id numeric(10,0),
    local_abbrev numeric(10,0),
    display_key character varying(60)
);


ALTER TABLE clinlims.project OWNER TO clinlims;

--
-- Name: COLUMN project.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.id IS 'Sequential number assigned by sequence';


--
-- Name: COLUMN project.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.name IS 'Short name of Project';


--
-- Name: COLUMN project.sys_user_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.sys_user_id IS 'Sequential Identifier';


--
-- Name: COLUMN project.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.description IS 'Description of Project';


--
-- Name: COLUMN project.started_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.started_date IS 'Start date of project';


--
-- Name: COLUMN project.completed_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.completed_date IS 'End date of project';


--
-- Name: COLUMN project.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.is_active IS 'Flag indicating if project is active';


--
-- Name: COLUMN project.reference_to; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.reference_to IS 'External reference information such as Grant number, contract number or purchase order number associated with this project.';


--
-- Name: COLUMN project.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project.display_key IS 'Resource file lookup key for localization of displaying the name';


--
-- Name: project_organization; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.project_organization (
    project_id numeric(10,0) NOT NULL,
    org_id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.project_organization OWNER TO clinlims;

--
-- Name: TABLE project_organization; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.project_organization IS 'many to many mapping table between project and organization';


--
-- Name: project_parameter; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.project_parameter (
    id numeric(10,0) NOT NULL,
    projparam_type character varying(1),
    operation character varying(2),
    value character varying(256),
    project_id numeric(10,0),
    param_name character varying(80)
);


ALTER TABLE clinlims.project_parameter OWNER TO clinlims;

--
-- Name: COLUMN project_parameter.projparam_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project_parameter.projparam_type IS 'Type of parameter: apply-parameter or search-parameter';


--
-- Name: COLUMN project_parameter.operation; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project_parameter.operation IS 'Operation to be performed: =, <, <=, >, >=,in,!=';


--
-- Name: COLUMN project_parameter.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.project_parameter.value IS 'Query or Set value';


--
-- Name: project_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.project_seq
    START WITH 5
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.project_seq OWNER TO clinlims;

--
-- Name: provider; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.provider (
    id numeric(10,0) NOT NULL,
    npi character varying(10),
    person_id numeric(10,0) NOT NULL,
    external_id character varying(50),
    provider_type character varying(1),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.provider OWNER TO clinlims;

--
-- Name: COLUMN provider.npi; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.provider.npi IS 'Unique National Provider ID';


--
-- Name: COLUMN provider.external_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.provider.external_id IS 'National/Local provider reference number such as UPIN or MINC#NIMC';


--
-- Name: COLUMN provider.provider_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.provider.provider_type IS 'Type of Provider (physician, nurse)';


--
-- Name: provider_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.provider_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.provider_seq OWNER TO clinlims;

--
-- Name: qa_event; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qa_event (
    id numeric(10,0) NOT NULL,
    name character varying(20),
    description character varying(120),
    is_billable character varying(1),
    reporting_sequence numeric,
    reporting_text character varying(1000),
    test_id numeric(10,0),
    is_holdable character varying(1) NOT NULL,
    lastupdated timestamp(6) without time zone,
    type numeric(10,0),
    category numeric(10,0),
    display_key character varying(60)
);


ALTER TABLE clinlims.qa_event OWNER TO clinlims;

--
-- Name: COLUMN qa_event.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_event.name IS 'Short Name';


--
-- Name: COLUMN qa_event.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_event.description IS 'Description of the QA event';


--
-- Name: COLUMN qa_event.is_billable; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_event.is_billable IS 'Indicates if analysis with this QA Event is billable';


--
-- Name: COLUMN qa_event.reporting_sequence; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_event.reporting_sequence IS 'An overall number that orders the print sequence';


--
-- Name: COLUMN qa_event.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_event.test_id IS 'Reported/printed text';


--
-- Name: COLUMN qa_event.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_event.display_key IS 'Resource file lookup key for localization of displaying the name';


--
-- Name: qa_event_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.qa_event_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.qa_event_seq OWNER TO clinlims;

--
-- Name: qa_observation; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qa_observation (
    id numeric(10,0) NOT NULL,
    observed_id numeric(10,0) NOT NULL,
    observed_type character varying(30) NOT NULL,
    qa_observation_type_id numeric(10,0) NOT NULL,
    value_type character varying(1) NOT NULL,
    value character varying(120) NOT NULL,
    lastupdated timestamp with time zone DEFAULT now()
);


ALTER TABLE clinlims.qa_observation OWNER TO clinlims;

--
-- Name: TABLE qa_observation; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.qa_observation IS 'The observation that are about sample/analysis qa_events.  Does not include data about the sample';


--
-- Name: COLUMN qa_observation.observed_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_observation.observed_id IS 'The row id for either sample_qaEvent or analysis_qaEvent';


--
-- Name: COLUMN qa_observation.observed_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_observation.observed_type IS 'One of ANALYSIS or SAMPLE';


--
-- Name: COLUMN qa_observation.qa_observation_type_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_observation.qa_observation_type_id IS 'The type of observation this is';


--
-- Name: COLUMN qa_observation.value_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_observation.value_type IS 'Dictionary, literal or localization key, D, L, K';


--
-- Name: COLUMN qa_observation.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qa_observation.value IS 'The actual value';


--
-- Name: qa_observation_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.qa_observation_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.qa_observation_seq OWNER TO clinlims;

--
-- Name: qa_observation_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qa_observation_type (
    id numeric(10,0) NOT NULL,
    name character varying(30) NOT NULL,
    description character varying(60),
    lastupdated timestamp with time zone DEFAULT now()
);


ALTER TABLE clinlims.qa_observation_type OWNER TO clinlims;

--
-- Name: TABLE qa_observation_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.qa_observation_type IS 'The types of observation that are about sample/analysis qa_events ';


--
-- Name: qa_observation_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.qa_observation_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.qa_observation_type_seq OWNER TO clinlims;

--
-- Name: qc; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qc (
    id numeric NOT NULL,
    uom_id numeric(10,0),
    sys_user_id numeric(10,0),
    name character varying(30),
    source character varying(30),
    lot_number character varying(30),
    prepared_date timestamp without time zone,
    prepared_volume numeric,
    usable_date timestamp without time zone,
    expire_date timestamp without time zone
);


ALTER TABLE clinlims.qc OWNER TO clinlims;

--
-- Name: COLUMN qc.sys_user_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc.sys_user_id IS 'Sequential Identifier';


--
-- Name: COLUMN qc.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc.name IS 'Descriptive QC name: Positive control for CHL';


--
-- Name: COLUMN qc.source; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc.source IS 'Name of supplier such as company or in-house';


--
-- Name: COLUMN qc.lot_number; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc.lot_number IS 'Lot number';


--
-- Name: COLUMN qc.prepared_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc.prepared_date IS 'Date QC was prepared';


--
-- Name: COLUMN qc.prepared_volume; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc.prepared_volume IS 'Amount prepared';


--
-- Name: COLUMN qc.usable_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc.usable_date IS 'Cannot be used before this date';


--
-- Name: COLUMN qc.expire_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc.expire_date IS 'Cannot be used after this date';


--
-- Name: qc_analytes; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qc_analytes (
    id numeric(10,0) NOT NULL,
    qcanaly_type character varying(1),
    value character varying(80),
    analyte_id numeric(10,0)
);


ALTER TABLE clinlims.qc_analytes OWNER TO clinlims;

--
-- Name: COLUMN qc_analytes.qcanaly_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc_analytes.qcanaly_type IS 'Type of value: dictionary, titer range, number range, true value';


--
-- Name: COLUMN qc_analytes.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.qc_analytes.value IS 'Min max,, true value & % if type is any range';


--
-- Name: qrtz_blob_triggers; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_blob_triggers (
    trigger_name character varying(80) NOT NULL,
    trigger_group character varying(80) NOT NULL,
    blob_data bytea
);


ALTER TABLE clinlims.qrtz_blob_triggers OWNER TO clinlims;

--
-- Name: qrtz_calendars; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_calendars (
    calendar_name character varying(80) NOT NULL,
    calendar bytea NOT NULL
);


ALTER TABLE clinlims.qrtz_calendars OWNER TO clinlims;

--
-- Name: qrtz_cron_triggers; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_cron_triggers (
    trigger_name character varying(80) NOT NULL,
    trigger_group character varying(80) NOT NULL,
    cron_expression character varying(80) NOT NULL,
    time_zone_id character varying(80)
);


ALTER TABLE clinlims.qrtz_cron_triggers OWNER TO clinlims;

--
-- Name: qrtz_fired_triggers; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_fired_triggers (
    entry_id character varying(95) NOT NULL,
    trigger_name character varying(80) NOT NULL,
    trigger_group character varying(80) NOT NULL,
    is_volatile boolean NOT NULL,
    instance_name character varying(80) NOT NULL,
    fired_time bigint NOT NULL,
    priority integer NOT NULL,
    state character varying(16) NOT NULL,
    job_name character varying(80),
    job_group character varying(80),
    is_stateful boolean,
    requests_recovery boolean
);


ALTER TABLE clinlims.qrtz_fired_triggers OWNER TO clinlims;

--
-- Name: qrtz_job_details; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_job_details (
    job_name character varying(80) NOT NULL,
    job_group character varying(80) NOT NULL,
    description character varying(120),
    job_class_name character varying(128) NOT NULL,
    is_durable boolean NOT NULL,
    is_volatile boolean NOT NULL,
    is_stateful boolean NOT NULL,
    requests_recovery boolean NOT NULL,
    job_data bytea
);


ALTER TABLE clinlims.qrtz_job_details OWNER TO clinlims;

--
-- Name: qrtz_job_listeners; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_job_listeners (
    job_name character varying(80) NOT NULL,
    job_group character varying(80) NOT NULL,
    job_listener character varying(80) NOT NULL
);


ALTER TABLE clinlims.qrtz_job_listeners OWNER TO clinlims;

--
-- Name: qrtz_locks; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_locks (
    lock_name character varying(40) NOT NULL
);


ALTER TABLE clinlims.qrtz_locks OWNER TO clinlims;

--
-- Name: qrtz_paused_trigger_grps; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_paused_trigger_grps (
    trigger_group character varying(80) NOT NULL
);


ALTER TABLE clinlims.qrtz_paused_trigger_grps OWNER TO clinlims;

--
-- Name: qrtz_scheduler_state; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_scheduler_state (
    instance_name character varying(80) NOT NULL,
    last_checkin_time bigint NOT NULL,
    checkin_interval bigint NOT NULL
);


ALTER TABLE clinlims.qrtz_scheduler_state OWNER TO clinlims;

--
-- Name: qrtz_simple_triggers; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_simple_triggers (
    trigger_name character varying(80) NOT NULL,
    trigger_group character varying(80) NOT NULL,
    repeat_count bigint NOT NULL,
    repeat_interval bigint NOT NULL,
    times_triggered bigint NOT NULL
);


ALTER TABLE clinlims.qrtz_simple_triggers OWNER TO clinlims;

--
-- Name: qrtz_trigger_listeners; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_trigger_listeners (
    trigger_name character varying(80) NOT NULL,
    trigger_group character varying(80) NOT NULL,
    trigger_listener character varying(80) NOT NULL
);


ALTER TABLE clinlims.qrtz_trigger_listeners OWNER TO clinlims;

--
-- Name: qrtz_triggers; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.qrtz_triggers (
    trigger_name character varying(80) NOT NULL,
    trigger_group character varying(80) NOT NULL,
    job_name character varying(80) NOT NULL,
    job_group character varying(80) NOT NULL,
    is_volatile boolean NOT NULL,
    description character varying(120),
    next_fire_time bigint,
    prev_fire_time bigint,
    priority integer,
    trigger_state character varying(16) NOT NULL,
    trigger_type character varying(8) NOT NULL,
    start_time bigint NOT NULL,
    end_time bigint,
    calendar_name character varying(80),
    misfire_instr smallint,
    job_data bytea
);


ALTER TABLE clinlims.qrtz_triggers OWNER TO clinlims;

--
-- Name: quartz_cron_scheduler; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.quartz_cron_scheduler (
    id numeric(10,0) NOT NULL,
    cron_statement character varying(32) DEFAULT 'never'::character varying NOT NULL,
    last_run timestamp with time zone,
    active boolean DEFAULT true,
    run_if_past boolean DEFAULT true,
    name character varying(40),
    job_name character varying(60) NOT NULL,
    display_key character varying(60),
    description_key character varying(60)
);


ALTER TABLE clinlims.quartz_cron_scheduler OWNER TO clinlims;

--
-- Name: TABLE quartz_cron_scheduler; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.quartz_cron_scheduler IS 'Sets up the quartz scheduler for cron jobs';


--
-- Name: COLUMN quartz_cron_scheduler.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.quartz_cron_scheduler.id IS 'Id for this schedule';


--
-- Name: COLUMN quartz_cron_scheduler.cron_statement; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.quartz_cron_scheduler.cron_statement IS 'The cron statement for when the job should run. N.B. the default is not a valid cron expression';


--
-- Name: COLUMN quartz_cron_scheduler.last_run; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.quartz_cron_scheduler.last_run IS 'The last time this job was run';


--
-- Name: COLUMN quartz_cron_scheduler.active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.quartz_cron_scheduler.active IS 'True if the schedule is active, false if it is suspended';


--
-- Name: COLUMN quartz_cron_scheduler.run_if_past; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.quartz_cron_scheduler.run_if_past IS 'True if the job should be run if the application is started after the run time and it has not run yet that day';


--
-- Name: COLUMN quartz_cron_scheduler.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.quartz_cron_scheduler.name IS 'The name for this job';


--
-- Name: COLUMN quartz_cron_scheduler.job_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.quartz_cron_scheduler.job_name IS 'The internal job name, should not be editible by end user';


--
-- Name: COLUMN quartz_cron_scheduler.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.quartz_cron_scheduler.display_key IS 'The localized name for this job';


--
-- Name: quartz_cron_scheduler_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.quartz_cron_scheduler_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.quartz_cron_scheduler_seq OWNER TO clinlims;

--
-- Name: receiver_code_element; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.receiver_code_element (
    id numeric(10,0) NOT NULL,
    identifier character varying(20),
    text character varying(60),
    code_system character varying(20),
    lastupdated timestamp(6) without time zone,
    message_org_id numeric(10,0),
    code_element_type_id numeric(10,0)
);


ALTER TABLE clinlims.receiver_code_element OWNER TO clinlims;

--
-- Name: receiver_code_element_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.receiver_code_element_seq
    START WITH 21
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.receiver_code_element_seq OWNER TO clinlims;

--
-- Name: reference_tables; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.reference_tables (
    id numeric(10,0) NOT NULL,
    name character varying(40),
    keep_history character varying(1),
    is_hl7_encoded character varying(1),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.reference_tables OWNER TO clinlims;

--
-- Name: COLUMN reference_tables.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.reference_tables.name IS 'Name of table or module';


--
-- Name: reference_tables_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.reference_tables_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.reference_tables_seq OWNER TO clinlims;

--
-- Name: referral; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.referral (
    id numeric(10,0) NOT NULL,
    analysis_id numeric(10,0),
    organization_id numeric(10,0),
    organization_name character varying(30),
    send_ready_date timestamp with time zone,
    sent_date timestamp with time zone,
    result_recieved_date timestamp with time zone,
    referral_reason_id numeric(10,0),
    referral_type_id numeric(10,0) NOT NULL,
    requester_name character varying(60),
    lastupdated timestamp with time zone,
    canceled boolean DEFAULT false,
    referral_request_date timestamp with time zone
);


ALTER TABLE clinlims.referral OWNER TO clinlims;

--
-- Name: TABLE referral; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.referral IS 'Tracks referrals made to and from the lab.  If referrals should be attached to samples add another column for sample and edit this comment';


--
-- Name: COLUMN referral.analysis_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.analysis_id IS 'The analysis which will be duplicated at other lab when refering out or which will be be done at this lab when referred in.  ';


--
-- Name: COLUMN referral.organization_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.organization_id IS 'The organization the sample was sent to or from';


--
-- Name: COLUMN referral.organization_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.organization_name IS 'The organiztion the sample was sent to or from, if busness rules allow them not to be in the organization table';


--
-- Name: COLUMN referral.send_ready_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.send_ready_date IS 'The date the referral out results are ready to be sent';


--
-- Name: COLUMN referral.sent_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.sent_date IS 'The date the referral out results are actually sent';


--
-- Name: COLUMN referral.result_recieved_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.result_recieved_date IS 'If this was a referral out then the date the results were recieved from the external lab';


--
-- Name: COLUMN referral.referral_reason_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.referral_reason_id IS 'Why was this referral done';


--
-- Name: COLUMN referral.requester_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.requester_name IS 'The name of the person who requested that the referral be done';


--
-- Name: COLUMN referral.referral_request_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral.referral_request_date IS 'The date the referral request was made';


--
-- Name: referral_reason; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.referral_reason (
    id numeric(10,0) NOT NULL,
    name character varying(30) NOT NULL,
    description character varying(60),
    display_key character varying(60),
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.referral_reason OWNER TO clinlims;

--
-- Name: TABLE referral_reason; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.referral_reason IS 'The reason a referral was made to or from the lab';


--
-- Name: COLUMN referral_reason.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_reason.name IS 'The name of the reason, default value displayed to user if no display_key value';


--
-- Name: COLUMN referral_reason.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_reason.description IS 'Clarification of the reason';


--
-- Name: COLUMN referral_reason.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_reason.display_key IS 'Key for localization files to display locale appropriate reasons';


--
-- Name: referral_reason_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.referral_reason_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.referral_reason_seq OWNER TO clinlims;

--
-- Name: referral_result; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.referral_result (
    id numeric(10,0) NOT NULL,
    referral_id numeric(10,0) NOT NULL,
    test_id numeric(10,0),
    result_id numeric(10,0),
    referral_report_date timestamp with time zone,
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.referral_result OWNER TO clinlims;

--
-- Name: TABLE referral_result; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.referral_result IS 'A referral may have one or more results';


--
-- Name: COLUMN referral_result.referral_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_result.referral_id IS 'The referral for which this is a result. May be one to many';


--
-- Name: COLUMN referral_result.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_result.test_id IS 'When the referral lab reported the results back';


--
-- Name: COLUMN referral_result.result_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_result.result_id IS 'The result returned by the referral lab';


--
-- Name: referral_result_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.referral_result_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.referral_result_seq OWNER TO clinlims;

--
-- Name: referral_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.referral_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.referral_seq OWNER TO clinlims;

--
-- Name: referral_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.referral_type (
    id numeric(10,0) NOT NULL,
    name character varying(30) NOT NULL,
    description character varying(60),
    display_key character varying(60),
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.referral_type OWNER TO clinlims;

--
-- Name: TABLE referral_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.referral_type IS 'The type of referral. i.e. a referral into the lab or a referral out of the lab';


--
-- Name: COLUMN referral_type.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_type.name IS 'The name of the type, default value displayed to user if no display_key value';


--
-- Name: COLUMN referral_type.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_type.description IS 'Clarification of the type';


--
-- Name: COLUMN referral_type.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referral_type.display_key IS 'Key for localization files to display locale appropriate types';


--
-- Name: referral_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.referral_type_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.referral_type_seq OWNER TO clinlims;

--
-- Name: referring_test_result; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.referring_test_result (
    id numeric NOT NULL,
    sample_item_id numeric NOT NULL,
    test_name text,
    result_value text,
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.referring_test_result OWNER TO clinlims;

--
-- Name: TABLE referring_test_result; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.referring_test_result IS 'Holds the test names and results of test done at referring lab.  Breaks reliance on test catalog';


--
-- Name: COLUMN referring_test_result.sample_item_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referring_test_result.sample_item_id IS 'Reference to the sample item on which the test was done';


--
-- Name: COLUMN referring_test_result.test_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referring_test_result.test_name IS 'The name of the test done at the referring lab';


--
-- Name: COLUMN referring_test_result.result_value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.referring_test_result.result_value IS 'The value of the result from the referring lab';


--
-- Name: referring_test_result_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.referring_test_result_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.referring_test_result_seq OWNER TO clinlims;

--
-- Name: region; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.region (
    id numeric(10,0) NOT NULL,
    region character varying(240) NOT NULL,
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.region OWNER TO clinlims;

--
-- Name: COLUMN region.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.region.id IS 'Sequential Number';


--
-- Name: COLUMN region.region; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.region.region IS 'Epidemiology Region Description used for MLS';


--
-- Name: region_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.region_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.region_seq OWNER TO clinlims;

--
-- Name: report; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.report (
    id integer NOT NULL,
    category character varying(200),
    sort_order integer,
    menu_element_id character varying(100),
    display_key character varying(200),
    name character varying(200),
    is_visible boolean
);


ALTER TABLE clinlims.report OWNER TO clinlims;

--
-- Name: report_external_export; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.report_external_export (
    id numeric(10,0) NOT NULL,
    event_date timestamp with time zone NOT NULL,
    collection_date timestamp with time zone NOT NULL,
    sent_date timestamp with time zone,
    type numeric(10,0) NOT NULL,
    data text,
    lastupdated timestamp with time zone,
    send_flag boolean DEFAULT true,
    bookkeeping text
);


ALTER TABLE clinlims.report_external_export OWNER TO clinlims;

--
-- Name: TABLE report_external_export; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.report_external_export IS 'Table for holding aggregated results to be sent to an external application';


--
-- Name: COLUMN report_external_export.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.id IS 'primary key';


--
-- Name: COLUMN report_external_export.event_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.event_date IS 'The date for which the information was collected.  Granularity assumed to be one day';


--
-- Name: COLUMN report_external_export.collection_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.collection_date IS 'The date on which the information was collected.';


--
-- Name: COLUMN report_external_export.sent_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.sent_date IS 'The date which the data was successfully sent';


--
-- Name: COLUMN report_external_export.type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.type IS 'The type of report this is';


--
-- Name: COLUMN report_external_export.data; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.data IS 'Formated data.  May be either JASON or xml.  Many datapoints per row';


--
-- Name: COLUMN report_external_export.lastupdated; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.lastupdated IS 'The last time the row was updated for any reason';


--
-- Name: COLUMN report_external_export.send_flag; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.send_flag IS 'The data is ready to be sent.  It may have already been sent once';


--
-- Name: COLUMN report_external_export.bookkeeping; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_export.bookkeeping IS 'Data which the application will need to record that this document has been sent';


--
-- Name: report_external_import; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.report_external_import (
    id numeric(10,0) NOT NULL,
    sending_site character varying(20) NOT NULL,
    event_date timestamp with time zone NOT NULL,
    recieved_date timestamp with time zone,
    type character varying(32) NOT NULL,
    updated_flag boolean DEFAULT false,
    data text,
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.report_external_import OWNER TO clinlims;

--
-- Name: TABLE report_external_import; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.report_external_import IS 'Table for holding aggregated results sent by an external application';


--
-- Name: COLUMN report_external_import.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_import.id IS 'primary key';


--
-- Name: COLUMN report_external_import.sending_site; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_import.sending_site IS 'The site which is sending the info';


--
-- Name: COLUMN report_external_import.event_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_import.event_date IS 'The date for which the information was collected.  Granularity assumed to be one day';


--
-- Name: COLUMN report_external_import.recieved_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_import.recieved_date IS 'The date which the data was received';


--
-- Name: COLUMN report_external_import.type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_import.type IS 'The type of report this is';


--
-- Name: COLUMN report_external_import.data; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_import.data IS 'Formated data.  May be either JASON or xml.  Many datapoints per row';


--
-- Name: COLUMN report_external_import.lastupdated; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.report_external_import.lastupdated IS 'The last time the row was updated for any reason';


--
-- Name: report_external_import_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.report_external_import_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.report_external_import_seq OWNER TO clinlims;

--
-- Name: report_id_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.report_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    MAXVALUE 2147483647
    CACHE 1;


ALTER TABLE clinlims.report_id_seq OWNER TO clinlims;

--
-- Name: report_queue_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.report_queue_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.report_queue_seq OWNER TO clinlims;

--
-- Name: report_queue_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.report_queue_type (
    id numeric(10,0) NOT NULL,
    name character varying(32) NOT NULL,
    description character varying(60)
);


ALTER TABLE clinlims.report_queue_type OWNER TO clinlims;

--
-- Name: report_queue_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.report_queue_type_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.report_queue_type_seq OWNER TO clinlims;

--
-- Name: requester_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.requester_type (
    id numeric(10,0) NOT NULL,
    requester_type character varying(20) NOT NULL
);


ALTER TABLE clinlims.requester_type OWNER TO clinlims;

--
-- Name: TABLE requester_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.requester_type IS 'The types of entities which can request test.  This table will be used by sample_requester so the type should map to table';


--
-- Name: COLUMN requester_type.requester_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.requester_type.requester_type IS 'The type. i.e. organization or provider';


--
-- Name: requester_type_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.requester_type_seq
    START WITH 2
    INCREMENT BY 1
    MINVALUE 2
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.requester_type_seq OWNER TO clinlims;

--
-- Name: result; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.result (
    id numeric(10,0) NOT NULL,
    analysis_id numeric(10,0),
    sort_order numeric,
    is_reportable character varying(1),
    result_type character varying(1),
    value character varying(200),
    analyte_id numeric(10,0),
    test_result_id numeric(10,0),
    lastupdated timestamp(6) without time zone,
    min_normal double precision,
    max_normal double precision,
    parent_id numeric(10,0),
    significant_digits numeric DEFAULT (0)::numeric,
    "grouping" numeric DEFAULT (0)::numeric
);


ALTER TABLE clinlims.result OWNER TO clinlims;

--
-- Name: COLUMN result.analysis_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.analysis_id IS 'Sequential number';


--
-- Name: COLUMN result.sort_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.sort_order IS 'The order the results are displayed (sort order)';


--
-- Name: COLUMN result.is_reportable; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.is_reportable IS 'Indicates if the result is reportable.';


--
-- Name: COLUMN result.result_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.result_type IS 'Type of result: Dictionary, titer, number, date';


--
-- Name: COLUMN result.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.value IS 'Actual result value.';


--
-- Name: COLUMN result.min_normal; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.min_normal IS 'The min normal value for this result. (May vary by patient sex and age)';


--
-- Name: COLUMN result.max_normal; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.max_normal IS 'The max normal value for this result. (May vary by patient sex and age)';


--
-- Name: COLUMN result.parent_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.parent_id IS 'The id of the result that this result is dependent on';


--
-- Name: COLUMN result.significant_digits; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result.significant_digits IS 'Significant digits for results at the time the results were entered';


--
-- Name: COLUMN result."grouping"; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result."grouping" IS 'When an analysis has more then one set of results this groups them';


--
-- Name: result_inventory; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.result_inventory (
    id numeric(10,0) NOT NULL,
    inventory_location_id numeric(10,0) NOT NULL,
    result_id numeric(10,0) NOT NULL,
    description character varying(20),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.result_inventory OWNER TO clinlims;

--
-- Name: TABLE result_inventory; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.result_inventory IS 'Table to link analyte, inventory_items and results.  This is to tie a specific test kit to HIV or syphilis test result.';


--
-- Name: COLUMN result_inventory.inventory_location_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_inventory.inventory_location_id IS 'The specific identifiable inventory.  For Haiti this should be a test kit in inventory';


--
-- Name: COLUMN result_inventory.result_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_inventory.result_id IS 'The result which is tied to the inventory';


--
-- Name: COLUMN result_inventory.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_inventory.description IS 'The description of inventory the result refers to.';


--
-- Name: result_inventory_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.result_inventory_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.result_inventory_seq OWNER TO clinlims;

--
-- Name: result_limits; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.result_limits (
    id numeric(10,0) NOT NULL,
    test_id numeric(10,0) NOT NULL,
    test_result_type_id numeric NOT NULL,
    min_age double precision DEFAULT 0,
    max_age double precision DEFAULT 'Infinity'::double precision,
    gender character(1),
    low_normal double precision DEFAULT '-Infinity'::double precision,
    high_normal double precision DEFAULT 'Infinity'::double precision,
    low_valid double precision DEFAULT '-Infinity'::double precision,
    high_valid double precision DEFAULT 'Infinity'::double precision,
    low_reporting_range double precision DEFAULT '-Infinity'::double precision,
    high_reporting_range double precision DEFAULT 'Infinity'::double precision,
    lastupdated timestamp(6) without time zone,
    normal_dictionary_id numeric(10,0),
    always_validate boolean DEFAULT false
);


ALTER TABLE clinlims.result_limits OWNER TO clinlims;

--
-- Name: TABLE result_limits; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.result_limits IS 'This is a mainly read only table for normal and valid limits for given tests.  Currently it assumes that only age and gender matter.  If more criteria matter then refactor';


--
-- Name: COLUMN result_limits.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.test_id IS 'Refers to the test table, this is additional information';


--
-- Name: COLUMN result_limits.test_result_type_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.test_result_type_id IS 'The data type of the results';


--
-- Name: COLUMN result_limits.min_age; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.min_age IS 'Should be null or less than max age';


--
-- Name: COLUMN result_limits.max_age; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.max_age IS 'Should be null or greater than min age';


--
-- Name: COLUMN result_limits.gender; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.gender IS 'Should be F or M or null if gender is not a criteria';


--
-- Name: COLUMN result_limits.low_normal; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.low_normal IS 'Low end of normal range';


--
-- Name: COLUMN result_limits.high_normal; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.high_normal IS 'High end of the normal range';


--
-- Name: COLUMN result_limits.low_valid; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.low_valid IS 'Low end of the valid range, any result value lower should be considered suspect';


--
-- Name: COLUMN result_limits.high_valid; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.high_valid IS 'high end of the valid range, any result value higher should be considered suspect';


--
-- Name: COLUMN result_limits.normal_dictionary_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.normal_dictionary_id IS 'Reference to the dictionary value which is normal for test';


--
-- Name: COLUMN result_limits.always_validate; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_limits.always_validate IS 'Is further validation always required no matter what the results';


--
-- Name: result_limits_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.result_limits_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.result_limits_seq OWNER TO clinlims;

--
-- Name: result_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.result_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.result_seq OWNER TO clinlims;

--
-- Name: result_signature; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.result_signature (
    id numeric(10,0) NOT NULL,
    result_id numeric(10,0) NOT NULL,
    system_user_id numeric(10,0),
    is_supervisor boolean DEFAULT false NOT NULL,
    lastupdated timestamp(6) without time zone,
    non_user_name character varying(20)
);


ALTER TABLE clinlims.result_signature OWNER TO clinlims;

--
-- Name: TABLE result_signature; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.result_signature IS 'This matches the person who signed the result form with the result.';


--
-- Name: COLUMN result_signature.result_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_signature.result_id IS 'The result which is being signed';


--
-- Name: COLUMN result_signature.system_user_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_signature.system_user_id IS 'The signer of the result';


--
-- Name: COLUMN result_signature.is_supervisor; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_signature.is_supervisor IS 'Is the signer a supervisor';


--
-- Name: COLUMN result_signature.non_user_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.result_signature.non_user_name IS 'For signers that are not systemUsers';


--
-- Name: result_signature_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.result_signature_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.result_signature_seq OWNER TO clinlims;

--
-- Name: sample; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample (
    id numeric(10,0) NOT NULL,
    accession_number character varying(20) NOT NULL,
    package_id numeric(10,0),
    domain character varying(1),
    next_item_sequence numeric(10,0),
    revision numeric,
    entered_date timestamp without time zone NOT NULL,
    received_date timestamp without time zone NOT NULL,
    collection_date timestamp without time zone,
    client_reference character varying(20),
    status character varying(1),
    released_date timestamp without time zone,
    sticker_rcvd_flag character varying(1),
    sys_user_id numeric(10,0),
    barcode character varying(20),
    transmission_date timestamp without time zone,
    lastupdated timestamp(6) without time zone,
    spec_or_isolate character varying(1),
    priority numeric(1,0),
    status_id numeric(10,0),
    referring_id character varying(50),
    clinical_order_id numeric(10,0),
    is_confirmation boolean DEFAULT false NOT NULL
);


ALTER TABLE clinlims.sample OWNER TO clinlims;

--
-- Name: COLUMN sample.status_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample.status_id IS 'foriegn key to status of analysis ';


--
-- Name: COLUMN sample.referring_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample.referring_id IS 'The external number supplied the referrer.  AKA Order number';


--
-- Name: sample_domain; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_domain (
    id numeric(10,0) NOT NULL,
    domain_description character varying(20),
    domain character varying(1),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.sample_domain OWNER TO clinlims;

--
-- Name: COLUMN sample_domain.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_domain.id IS 'A unique auto generated integer number assigned by the database.';


--
-- Name: COLUMN sample_domain.domain_description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_domain.domain_description IS 'Type of sample this can be applied to:environ, human, animal, rabies, bt, newborn.';


--
-- Name: COLUMN sample_domain.domain; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_domain.domain IS 'Code for description: E-Environmental, A-Animal, C-Clinical';


--
-- Name: sample_domain_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_domain_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_domain_seq OWNER TO clinlims;

--
-- Name: sample_environmental; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_environmental (
    id numeric(10,0) NOT NULL,
    samp_id numeric(10,0) NOT NULL,
    is_hazardous character varying(1),
    lot_nbr character varying(30),
    description character varying(40),
    chem_samp_num character varying(240),
    street_address character varying(30),
    multiple_unit character varying(30),
    city character varying(30),
    state character varying(2),
    zip_code character varying(10),
    country character varying(20),
    collector character varying(40),
    sampling_location character varying(40)
);


ALTER TABLE clinlims.sample_environmental OWNER TO clinlims;

--
-- Name: COLUMN sample_environmental.samp_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_environmental.samp_id IS 'MDH Specimen Number';


--
-- Name: COLUMN sample_environmental.lot_nbr; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_environmental.lot_nbr IS 'If sample is unopened package of food then include the lot number from the package';


--
-- Name: COLUMN sample_environmental.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_environmental.description IS 'Additional description field for sample attributes.';


--
-- Name: COLUMN sample_environmental.zip_code; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_environmental.zip_code IS 'Zip +4 code';


--
-- Name: COLUMN sample_environmental.collector; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_environmental.collector IS 'Person collecting the sample';


--
-- Name: COLUMN sample_environmental.sampling_location; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_environmental.sampling_location IS 'Sampling location - name of restaurant, store, farm';


--
-- Name: sample_human; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_human (
    id numeric(10,0) NOT NULL,
    provider_id numeric(10,0),
    samp_id numeric(10,0) NOT NULL,
    patient_id numeric(10,0),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.sample_human OWNER TO clinlims;

--
-- Name: COLUMN sample_human.samp_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_human.samp_id IS 'MDH Specimen Number';


--
-- Name: sample_human_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_human_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_human_seq OWNER TO clinlims;

--
-- Name: sample_item; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_item (
    id numeric(10,0) NOT NULL,
    sort_order numeric NOT NULL,
    sampitem_id numeric(10,0),
    samp_id numeric(10,0),
    source_id numeric(10,0),
    typeosamp_id numeric(10,0),
    uom_id numeric(10,0),
    source_other character varying(40),
    quantity numeric,
    lastupdated timestamp(6) without time zone,
    external_id character varying(20),
    collection_date timestamp with time zone,
    status_id numeric(10,0) NOT NULL,
    collector character varying(60)
);


ALTER TABLE clinlims.sample_item OWNER TO clinlims;

--
-- Name: COLUMN sample_item.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.id IS 'Sample source write in if not already defined';


--
-- Name: COLUMN sample_item.sort_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.sort_order IS 'Sample items unique sequence number for this sample';


--
-- Name: COLUMN sample_item.sampitem_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.sampitem_id IS 'Sample source write in if not already defined';


--
-- Name: COLUMN sample_item.samp_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.samp_id IS 'MDH Specimen Number';


--
-- Name: COLUMN sample_item.source_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.source_id IS 'A unique auto generated integer number assigned by the database.';


--
-- Name: COLUMN sample_item.typeosamp_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.typeosamp_id IS 'A unique auto generated integer number assigned by the database';


--
-- Name: COLUMN sample_item.source_other; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.source_other IS 'Sample source write in if not already defined';


--
-- Name: COLUMN sample_item.quantity; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.quantity IS 'Amount of sample';


--
-- Name: COLUMN sample_item.external_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.external_id IS 'An external id that may have been attached to the sample item before it came to the lab';


--
-- Name: COLUMN sample_item.collection_date; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.collection_date IS 'The date this sample_item was collected or seperated from other part of sample';


--
-- Name: COLUMN sample_item.status_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.status_id IS 'The status of this sample item';


--
-- Name: COLUMN sample_item.collector; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_item.collector IS 'The name of the person who collected the sample';


--
-- Name: sample_item_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_item_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_item_seq OWNER TO clinlims;

--
-- Name: sample_newborn; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_newborn (
    id numeric(10,0) NOT NULL,
    weight numeric(5,0),
    multi_birth character(1),
    birth_order numeric(2,0),
    gestational_week numeric(5,2),
    date_first_feeding date,
    breast character(1),
    tpn character(1),
    formula character(1),
    milk character(1),
    soy character(1),
    jaundice character(1),
    antibiotics character(1),
    transfused character(1),
    date_transfusion date,
    medical_record_numeric character varying(18),
    nicu character(1),
    birth_defect character(1),
    pregnancy_complication character(1),
    deceased_sibling character(1),
    cause_of_death character varying(50),
    family_history character(1),
    other character varying(100),
    y_numeric character varying(18),
    yellow_card character(1),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.sample_newborn OWNER TO clinlims;

--
-- Name: sample_org_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_org_seq
    START WITH 112
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_org_seq OWNER TO clinlims;

--
-- Name: sample_organization; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_organization (
    id numeric(10,0) NOT NULL,
    org_id numeric(10,0),
    samp_id numeric(10,0),
    samp_org_type character varying(1),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.sample_organization OWNER TO clinlims;

--
-- Name: COLUMN sample_organization.org_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_organization.org_id IS 'Sequential Numbering Field';


--
-- Name: COLUMN sample_organization.samp_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_organization.samp_id IS 'MDH Specimen Number';


--
-- Name: COLUMN sample_organization.samp_org_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_organization.samp_org_type IS 'Type of organization: Primary, Secondary, Billing';


--
-- Name: sample_pdf; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_pdf (
    id numeric(10,0) NOT NULL,
    accession_number numeric(10,0) NOT NULL,
    allow_view character varying(1),
    barcode character varying(20)
);


ALTER TABLE clinlims.sample_pdf OWNER TO clinlims;

--
-- Name: sample_pdf_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_pdf_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.sample_pdf_seq OWNER TO clinlims;

--
-- Name: sample_proj_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_proj_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_proj_seq OWNER TO clinlims;

--
-- Name: sample_projects; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_projects (
    samp_id numeric(10,0) NOT NULL,
    proj_id numeric(10,0),
    is_permanent character varying(1),
    id numeric(10,0) NOT NULL,
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.sample_projects OWNER TO clinlims;

--
-- Name: COLUMN sample_projects.samp_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_projects.samp_id IS 'MDH Specimen Number';


--
-- Name: COLUMN sample_projects.proj_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_projects.proj_id IS 'Sequential number assigned by sequence';


--
-- Name: COLUMN sample_projects.is_permanent; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_projects.is_permanent IS 'Indicates if project is assigned to this sample permanently (Y/N)';


--
-- Name: sample_qaevent; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_qaevent (
    id numeric(10,0) NOT NULL,
    qa_event_id numeric(10,0),
    sample_id numeric(10,0),
    completed_date date,
    lastupdated timestamp without time zone,
    sampleitem_id numeric(10,0),
    entered_date timestamp with time zone
);


ALTER TABLE clinlims.sample_qaevent OWNER TO clinlims;

--
-- Name: COLUMN sample_qaevent.sampleitem_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_qaevent.sampleitem_id IS 'If the qaevent refers to a sampleitem of the sample use this column';


--
-- Name: sample_qaevent_action; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_qaevent_action (
    id numeric(10,0) NOT NULL,
    sample_qaevent_id numeric(10,0) NOT NULL,
    action_id numeric(10,0) NOT NULL,
    created_date date NOT NULL,
    lastupdated timestamp(6) without time zone,
    sys_user_id numeric(10,0)
);


ALTER TABLE clinlims.sample_qaevent_action OWNER TO clinlims;

--
-- Name: sample_qaevent_action_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_qaevent_action_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_qaevent_action_seq OWNER TO clinlims;

--
-- Name: sample_qaevent_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_qaevent_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_qaevent_seq OWNER TO clinlims;

--
-- Name: sample_requester; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sample_requester (
    sample_id numeric(10,0) NOT NULL,
    requester_id numeric(10,0) NOT NULL,
    requester_type_id numeric(10,0) NOT NULL,
    lastupdated timestamp(6) without time zone,
    id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.sample_requester OWNER TO clinlims;

--
-- Name: TABLE sample_requester; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.sample_requester IS 'Links a sample to the entity which requested it';


--
-- Name: COLUMN sample_requester.sample_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_requester.sample_id IS 'The sample';


--
-- Name: COLUMN sample_requester.requester_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_requester.requester_id IS 'The requester_id.  The exact table row depends on the requester type';


--
-- Name: COLUMN sample_requester.requester_type_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.sample_requester.requester_type_id IS 'The type from the requester_type table';


--
-- Name: sample_requester_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_requester_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_requester_seq OWNER TO clinlims;

--
-- Name: sample_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_seq OWNER TO clinlims;

--
-- Name: sample_type_panel_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_type_panel_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_type_panel_seq OWNER TO clinlims;

--
-- Name: sample_type_test_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.sample_type_test_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.sample_type_test_seq OWNER TO clinlims;

--
-- Name: sampletype_panel; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sampletype_panel (
    id numeric(10,0) NOT NULL,
    sample_type_id numeric(10,0) NOT NULL,
    panel_id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.sampletype_panel OWNER TO clinlims;

--
-- Name: sampletype_test; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.sampletype_test (
    id numeric(10,0) NOT NULL,
    sample_type_id numeric(10,0) NOT NULL,
    test_id numeric(10,0) NOT NULL,
    is_panel boolean DEFAULT false
);


ALTER TABLE clinlims.sampletype_test OWNER TO clinlims;

--
-- Name: scriptlet; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.scriptlet (
    id numeric(10,0) NOT NULL,
    name character varying(40),
    code_type character varying(1),
    code_source character varying(4000),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.scriptlet OWNER TO clinlims;

--
-- Name: COLUMN scriptlet.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.scriptlet.name IS 'Script name';


--
-- Name: COLUMN scriptlet.code_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.scriptlet.code_type IS 'Flag indicating type of script code : Java, Basic, PLSQL';


--
-- Name: COLUMN scriptlet.lastupdated; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.scriptlet.lastupdated IS 'Body of Source Code';


--
-- Name: scriptlet_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.scriptlet_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.scriptlet_seq OWNER TO clinlims;

--
-- Name: site_information; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.site_information (
    id integer NOT NULL,
    name character varying(32) NOT NULL,
    lastupdated timestamp with time zone,
    description character varying(120),
    value character varying(200),
    encrypted boolean DEFAULT false,
    domain_id numeric(10,0),
    value_type character varying(10) DEFAULT 'text'::character varying NOT NULL,
    instruction_key character varying(40),
    "group" numeric DEFAULT (0)::numeric,
    schedule_id numeric(10,0),
    tag character varying(20),
    dictionary_category_id numeric(10,0),
    description_key character varying(42),
    name_key character varying
);


ALTER TABLE clinlims.site_information OWNER TO clinlims;

--
-- Name: TABLE site_information; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.site_information IS 'Information about a specific installation at a site, seperate from an implimentation';


--
-- Name: COLUMN site_information.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.name IS 'Name by which this information will be found';


--
-- Name: COLUMN site_information.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.description IS 'Clarification of the name';


--
-- Name: COLUMN site_information.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.value IS 'Value for the named information';


--
-- Name: COLUMN site_information.encrypted; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.encrypted IS 'Is the value an encrypted value.  Used for passwords';


--
-- Name: COLUMN site_information.value_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.value_type IS 'The type of value which can be specified for the value. Currently either ''boolean'',''text'',''dictionary'',''logoUpload'' or ''complex'' ';


--
-- Name: COLUMN site_information.instruction_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.instruction_key IS 'The key in Message_Resource which give the user the text for the meaning and consequences of the information';


--
-- Name: COLUMN site_information."group"; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information."group" IS 'If items should be grouped together when displaying they should have the same group number';


--
-- Name: COLUMN site_information.schedule_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.schedule_id IS 'quartz_cron_scheduler id if the item is associated with a scheduler ';


--
-- Name: COLUMN site_information.tag; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.tag IS 'A tag to help determine how the information should be used';


--
-- Name: COLUMN site_information.dictionary_category_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.site_information.dictionary_category_id IS 'Value of the dictionary category if the type of record is dictionary';


--
-- Name: site_information_domain; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.site_information_domain (
    id numeric(10,0) NOT NULL,
    name character varying(20) NOT NULL,
    description character varying(120)
);


ALTER TABLE clinlims.site_information_domain OWNER TO clinlims;

--
-- Name: TABLE site_information_domain; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.site_information_domain IS 'Marks the domains to which site information belongs.  Intended use is administration pages';


--
-- Name: site_information_domain_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.site_information_domain_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.site_information_domain_seq OWNER TO clinlims;

--
-- Name: site_information_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.site_information_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.site_information_seq OWNER TO clinlims;

--
-- Name: source_of_sample; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.source_of_sample (
    id numeric(10,0) NOT NULL,
    description character varying(40),
    domain character varying(1),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.source_of_sample OWNER TO clinlims;

--
-- Name: COLUMN source_of_sample.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.source_of_sample.id IS 'A unique auto generated integer number assigned by the database.';


--
-- Name: COLUMN source_of_sample.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.source_of_sample.description IS 'Description such as left ear, right hand, kitchen sink.';


--
-- Name: COLUMN source_of_sample.domain; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.source_of_sample.domain IS 'Type of sample this can be applied to: Environ, Animal, Clinical';


--
-- Name: source_of_sample_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.source_of_sample_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.source_of_sample_seq OWNER TO clinlims;

--
-- Name: state_code; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.state_code (
    id numeric(10,0) NOT NULL,
    code character varying(240),
    description character varying(240),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.state_code OWNER TO clinlims;

--
-- Name: COLUMN state_code.code; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.state_code.code IS 'State abbreviation';


--
-- Name: COLUMN state_code.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.state_code.description IS 'State Name';


--
-- Name: state_code_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.state_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.state_code_seq OWNER TO clinlims;

--
-- Name: status_of_sample; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.status_of_sample (
    id numeric(10,0) NOT NULL,
    description character varying(240),
    code numeric(3,0) NOT NULL,
    status_type character varying(24) NOT NULL,
    lastupdated timestamp(6) without time zone,
    name character varying(30),
    display_key character varying(60),
    is_active character varying(1) DEFAULT 'Y'::character varying
);


ALTER TABLE clinlims.status_of_sample OWNER TO clinlims;

--
-- Name: COLUMN status_of_sample.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.status_of_sample.is_active IS 'Either Y or N';


--
-- Name: status_of_sample_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.status_of_sample_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.status_of_sample_seq OWNER TO clinlims;

--
-- Name: storage_location; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.storage_location (
    id numeric(10,0) NOT NULL,
    sort_order numeric,
    name character varying(20),
    location character varying(80),
    is_available character varying(1),
    parent_storageloc_id numeric(10,0),
    storage_unit_id numeric(10,0)
);


ALTER TABLE clinlims.storage_location OWNER TO clinlims;

--
-- Name: COLUMN storage_location.sort_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.storage_location.sort_order IS 'The sequence order of this item; sort order used for display';


--
-- Name: COLUMN storage_location.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.storage_location.name IS 'Name of unit: Virology Fridge #1';


--
-- Name: COLUMN storage_location.location; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.storage_location.location IS 'Location of storage';


--
-- Name: COLUMN storage_location.is_available; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.storage_location.is_available IS 'Indicates if storage is available for use.';


--
-- Name: storage_unit; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.storage_unit (
    id numeric(10,0) NOT NULL,
    category character varying(15),
    description character varying(60),
    is_singular character varying(1)
);


ALTER TABLE clinlims.storage_unit OWNER TO clinlims;

--
-- Name: COLUMN storage_unit.category; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.storage_unit.category IS 'type of storage unit: box, fridge, shelf, tube';


--
-- Name: COLUMN storage_unit.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.storage_unit.description IS 'Description of unit: 10 mL tube, 5 shelf fridge.';


--
-- Name: COLUMN storage_unit.is_singular; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.storage_unit.is_singular IS 'Y, N flag indicating if this unit can contain more than one item.';


--
-- Name: system_module; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_module (
    id numeric(10,0) NOT NULL,
    name character varying,
    description character varying,
    has_select_flag character varying(1),
    has_add_flag character varying(1),
    has_update_flag character varying(1),
    has_delete_flag character varying(1)
);


ALTER TABLE clinlims.system_module OWNER TO clinlims;

--
-- Name: COLUMN system_module.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_module.name IS 'Name of security module';


--
-- Name: COLUMN system_module.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_module.description IS 'Description for this security module';


--
-- Name: COLUMN system_module.has_select_flag; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_module.has_select_flag IS 'Flag indicating if this module can be assigned to a user';


--
-- Name: COLUMN system_module.has_add_flag; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_module.has_add_flag IS 'Flag indicating if this module has add capability';


--
-- Name: COLUMN system_module.has_update_flag; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_module.has_update_flag IS 'Flag indicating if this module has update capability';


--
-- Name: COLUMN system_module.has_delete_flag; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_module.has_delete_flag IS 'Flag indicating if this module has delete capability';


--
-- Name: system_module_param; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_module_param (
    id numeric(10,0) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE clinlims.system_module_param OWNER TO clinlims;

--
-- Name: system_module_param_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.system_module_param_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.system_module_param_seq OWNER TO clinlims;

--
-- Name: system_module_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.system_module_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.system_module_seq OWNER TO clinlims;

--
-- Name: system_module_url; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_module_url (
    id numeric(10,0) NOT NULL,
    url_path character varying(255) NOT NULL,
    system_module_id numeric(10,0) NOT NULL,
    system_module_param_id numeric(10,0)
);


ALTER TABLE clinlims.system_module_url OWNER TO clinlims;

--
-- Name: system_module_url_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.system_module_url_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.system_module_url_seq OWNER TO clinlims;

--
-- Name: system_role; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_role (
    id numeric(10,0) NOT NULL,
    name character(30) NOT NULL,
    description character varying(80),
    is_grouping_role boolean DEFAULT false,
    grouping_parent numeric(10,0),
    display_key character varying(60),
    active boolean DEFAULT true,
    editable boolean DEFAULT false
);


ALTER TABLE clinlims.system_role OWNER TO clinlims;

--
-- Name: TABLE system_role; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.system_role IS 'Describes the roles a user may have.  ';


--
-- Name: COLUMN system_role.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_role.name IS 'The name of the role, this is how it will appear to the user';


--
-- Name: COLUMN system_role.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_role.description IS 'Notes about the role';


--
-- Name: COLUMN system_role.is_grouping_role; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_role.is_grouping_role IS 'Indicates that this role is only for grouping other roles.  It should not have modules assigned to it';


--
-- Name: COLUMN system_role.grouping_parent; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_role.grouping_parent IS 'Should only refer to a grouping role';


--
-- Name: COLUMN system_role.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_role.display_key IS 'key for localizing dropdown lists';


--
-- Name: COLUMN system_role.active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_role.active IS 'Is this role active for this installation';


--
-- Name: COLUMN system_role.editable; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_role.editable IS 'Is this a role that can be de/activated by the user';


--
-- Name: system_role_module; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_role_module (
    id numeric(10,0) NOT NULL,
    has_select character varying(1),
    has_add character varying(1),
    has_update character varying(1),
    has_delete character varying(1),
    system_role_id numeric(10,0) NOT NULL,
    system_module_id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.system_role_module OWNER TO clinlims;

--
-- Name: system_role_module_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.system_role_module_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.system_role_module_seq OWNER TO clinlims;

--
-- Name: system_role_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.system_role_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.system_role_seq OWNER TO clinlims;

--
-- Name: system_user; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_user (
    id numeric(10,0) NOT NULL,
    external_id character varying(80),
    login_name character varying(20) NOT NULL,
    last_name character varying(30) NOT NULL,
    first_name character varying(20) NOT NULL,
    initials character varying(3),
    is_active character varying(1) NOT NULL,
    is_employee character varying(1) NOT NULL,
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.system_user OWNER TO clinlims;

--
-- Name: COLUMN system_user.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user.id IS 'Sequential Identifier';


--
-- Name: COLUMN system_user.external_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user.external_id IS 'External ID such as employee number or external system ID.';


--
-- Name: COLUMN system_user.login_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user.login_name IS 'User''s system log in name.';


--
-- Name: COLUMN system_user.last_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user.last_name IS 'Last name of person';


--
-- Name: COLUMN system_user.first_name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user.first_name IS 'Person Name';


--
-- Name: COLUMN system_user.initials; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user.initials IS 'Middle Initial';


--
-- Name: COLUMN system_user.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user.is_active IS 'Indicates the status of active or inactive for user';


--
-- Name: COLUMN system_user.is_employee; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user.is_employee IS 'Indicates if user is an MDH employee';


--
-- Name: system_user_module; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_user_module (
    id numeric(10,0) NOT NULL,
    has_select character varying(1),
    has_add character varying(1),
    has_update character varying(1),
    has_delete character varying(1),
    system_user_id numeric(10,0),
    system_module_id numeric(10,0)
);


ALTER TABLE clinlims.system_user_module OWNER TO clinlims;

--
-- Name: COLUMN system_user_module.has_select; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_module.has_select IS 'Flag indicating if this user has permission to enter this module';


--
-- Name: COLUMN system_user_module.has_add; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_module.has_add IS 'Flag indicating if this user has permission to add a record';


--
-- Name: COLUMN system_user_module.has_update; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_module.has_update IS 'Flag indicating if this person has permission to update a record';


--
-- Name: COLUMN system_user_module.has_delete; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_module.has_delete IS 'Flag indicating if this person has permission to remove a record';


--
-- Name: system_user_module_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.system_user_module_seq
    START WITH 0
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.system_user_module_seq OWNER TO clinlims;

--
-- Name: system_user_role; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_user_role (
    system_user_id numeric(10,0) NOT NULL,
    role_id numeric(10,0) NOT NULL
);


ALTER TABLE clinlims.system_user_role OWNER TO clinlims;

--
-- Name: system_user_section; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.system_user_section (
    id numeric NOT NULL,
    has_view character varying(1),
    has_assign character varying(1),
    has_complete character varying(1),
    has_release character varying(1),
    has_cancel character varying(1),
    system_user_id numeric(10,0),
    test_section_id numeric(10,0)
);


ALTER TABLE clinlims.system_user_section OWNER TO clinlims;

--
-- Name: COLUMN system_user_section.has_view; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_section.has_view IS 'Flag indicating if user has permission to iew this sections''s records';


--
-- Name: COLUMN system_user_section.has_assign; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_section.has_assign IS 'Flag indicating if user has permission to assign this section''s tests';


--
-- Name: COLUMN system_user_section.has_complete; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_section.has_complete IS 'Flag indicating if user has permission to complete this section''s tests';


--
-- Name: COLUMN system_user_section.has_release; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_section.has_release IS 'Flag indicating if user has permission to release this section''s tests';


--
-- Name: COLUMN system_user_section.has_cancel; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.system_user_section.has_cancel IS 'Flag indicating if user has permission to cancel this section''s tests';


--
-- Name: system_user_section_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.system_user_section_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.system_user_section_seq OWNER TO clinlims;

--
-- Name: system_user_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.system_user_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.system_user_seq OWNER TO clinlims;

--
-- Name: test; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test (
    id numeric(10,0) NOT NULL,
    method_id numeric(10,0),
    uom_id numeric(10,0),
    description character varying NOT NULL,
    loinc character varying(240),
    reporting_description character varying,
    sticker_req_flag character varying(1),
    is_active character varying(1),
    active_begin timestamp without time zone,
    active_end timestamp without time zone,
    is_reportable character varying(1),
    time_holding numeric,
    time_wait numeric,
    time_ta_average numeric,
    time_ta_warning numeric,
    time_ta_max numeric,
    label_qty numeric,
    lastupdated timestamp(6) without time zone,
    label_id numeric(10,0),
    test_trailer_id numeric(10,0),
    test_section_id numeric(10,0),
    scriptlet_id numeric(10,0),
    test_format_id numeric(10,0),
    local_code character varying,
    sort_order numeric DEFAULT 2147483647,
    name character varying NOT NULL,
    orderable boolean DEFAULT true,
    guid character varying(128) NOT NULL,
    name_localization_id numeric(10,0),
    reporting_name_localization_id numeric(10,0)
);


ALTER TABLE clinlims.test OWNER TO clinlims;

--
-- Name: COLUMN test.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.id IS 'Sequential value assigned on insert';


--
-- Name: COLUMN test.method_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.method_id IS 'Sequential number';


--
-- Name: COLUMN test.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.description IS 'Description for test';


--
-- Name: COLUMN test.reporting_description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.reporting_description IS 'Description for test that appears on reports';


--
-- Name: COLUMN test.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.is_active IS 'Active status flag';


--
-- Name: COLUMN test.active_begin; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.active_begin IS 'Active end date';


--
-- Name: COLUMN test.active_end; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.active_end IS 'Active begin date';


--
-- Name: COLUMN test.is_reportable; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.is_reportable IS 'The default flag indicating if ths test is reportable';


--
-- Name: COLUMN test.time_holding; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.time_holding IS 'Max hours between collection and received time';


--
-- Name: COLUMN test.time_wait; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.time_wait IS 'Hours to wait before analysis can begin';


--
-- Name: COLUMN test.time_ta_average; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.time_ta_average IS 'Average hours for test to be reported';


--
-- Name: COLUMN test.time_ta_warning; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.time_ta_warning IS 'Hours before issuing touraround warning for test not reported';


--
-- Name: COLUMN test.time_ta_max; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.time_ta_max IS 'Max hours test should be in laboratory';


--
-- Name: COLUMN test.label_qty; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.label_qty IS 'Number of labels to print';


--
-- Name: COLUMN test.orderable; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.orderable IS 'Should this test show in list of tests which can be ordered.  If not it is a reflex only test';


--
-- Name: COLUMN test.guid; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.guid IS 'The positive identifier for this test in the OpenELIS ecosystem';


--
-- Name: COLUMN test.name_localization_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.name_localization_id IS 'reference to the localization record for the test section
            name';


--
-- Name: COLUMN test.reporting_name_localization_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test.reporting_name_localization_id IS 'reference to the localization record for the reporting name';


--
-- Name: test_analyte; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_analyte (
    id numeric(10,0) NOT NULL,
    test_id numeric(10,0),
    analyte_id numeric(10,0),
    result_group numeric,
    sort_order numeric,
    testalyt_type character varying(1),
    lastupdated timestamp(6) without time zone,
    is_reportable character varying(1)
);


ALTER TABLE clinlims.test_analyte OWNER TO clinlims;

--
-- Name: COLUMN test_analyte.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_analyte.test_id IS 'Sequential value assigned on insert';


--
-- Name: COLUMN test_analyte.result_group; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_analyte.result_group IS 'A program generated group number';


--
-- Name: COLUMN test_analyte.sort_order; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_analyte.sort_order IS 'The order in which the analytes are displayed (sort order)';


--
-- Name: COLUMN test_analyte.testalyt_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_analyte.testalyt_type IS 'Type of analyte: required...';


--
-- Name: test_analyte_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.test_analyte_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.test_analyte_seq OWNER TO clinlims;

--
-- Name: test_code; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_code (
    test_id numeric(10,0) NOT NULL,
    code_type_id numeric(10,0) NOT NULL,
    value character varying(20) NOT NULL,
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.test_code OWNER TO clinlims;

--
-- Name: TABLE test_code; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.test_code IS 'For a given test and schema it gives the encoding';


--
-- Name: COLUMN test_code.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_code.test_id IS 'The test for which the coding supports. FK to test table.';


--
-- Name: COLUMN test_code.code_type_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_code.code_type_id IS 'The coding type id of the code';


--
-- Name: COLUMN test_code.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_code.value IS 'The actual code';


--
-- Name: test_code_type; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_code_type (
    id numeric(10,0) NOT NULL,
    schema_name character varying(32) NOT NULL,
    lastupdated timestamp with time zone
);


ALTER TABLE clinlims.test_code_type OWNER TO clinlims;

--
-- Name: TABLE test_code_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.test_code_type IS 'The names of the encoding schems supported (SNOMWD, LOINC etc)';


--
-- Name: test_dictionary; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_dictionary (
    id numeric(10,0) NOT NULL,
    test_id numeric(10,0) NOT NULL,
    dictionary_category_id numeric(10,0) NOT NULL,
    context character varying(20) NOT NULL,
    qualifiable_entry_id numeric(10,0),
    lastupdated timestamp with time zone DEFAULT now()
);


ALTER TABLE clinlims.test_dictionary OWNER TO clinlims;

--
-- Name: TABLE test_dictionary; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON TABLE clinlims.test_dictionary IS 'Links between tests and dictionary categories.  Intended use is for when a user might have to select dictioanry values for some asspect of a test';


--
-- Name: COLUMN test_dictionary.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_dictionary.test_id IS 'The test part of the linkage';


--
-- Name: COLUMN test_dictionary.dictionary_category_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_dictionary.dictionary_category_id IS 'The dictionary category part of the linkage';


--
-- Name: COLUMN test_dictionary.context; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_dictionary.context IS 'The context of the linkage.  Make it literate';


--
-- Name: COLUMN test_dictionary.qualifiable_entry_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_dictionary.qualifiable_entry_id IS 'This value has to be further qualified.  It''s intended value is ''other''';


--
-- Name: test_dictionary_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.test_dictionary_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.test_dictionary_seq OWNER TO clinlims;

--
-- Name: test_formats; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_formats (
    id numeric(10,0) NOT NULL,
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.test_formats OWNER TO clinlims;

--
-- Name: test_reflex; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_reflex (
    id numeric(10,0) NOT NULL,
    tst_rslt_id numeric,
    flags character varying(10),
    lastupdated timestamp(6) without time zone,
    test_analyte_id numeric(10,0),
    test_id numeric(10,0),
    add_test_id numeric(10,0),
    sibling_reflex numeric(10,0),
    scriptlet_id numeric(10,0)
);


ALTER TABLE clinlims.test_reflex OWNER TO clinlims;

--
-- Name: COLUMN test_reflex.flags; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_reflex.flags IS 'A string of 1 character codes: duplicate, auto-add';


--
-- Name: COLUMN test_reflex.sibling_reflex; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_reflex.sibling_reflex IS 'Reference to tests and results for reflexes with more than one condition.  All add_test_ids should be the same';


--
-- Name: COLUMN test_reflex.scriptlet_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_reflex.scriptlet_id IS 'If a non-test action should be taken then reference the scriptlet which says what to do';


--
-- Name: test_reflex_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.test_reflex_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.test_reflex_seq OWNER TO clinlims;

--
-- Name: test_result; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_result (
    id numeric NOT NULL,
    test_id numeric(10,0) NOT NULL,
    result_group numeric,
    flags character varying(10),
    tst_rslt_type character varying(1),
    value character varying(80),
    significant_digits numeric DEFAULT 0,
    quant_limit character varying(30),
    cont_level character varying(30),
    lastupdated timestamp(6) without time zone,
    scriptlet_id numeric(10,0),
    sort_order numeric(22,0),
    is_quantifiable boolean DEFAULT false,
    is_active boolean DEFAULT true,
    is_normal boolean
);


ALTER TABLE clinlims.test_result OWNER TO clinlims;

--
-- Name: COLUMN test_result.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.test_id IS 'Sequential value assigned on insert';


--
-- Name: COLUMN test_result.result_group; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.result_group IS 'the test_analyte result_group number';


--
-- Name: COLUMN test_result.flags; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.flags IS 'a string of 1 character codes: Positive, Reportable...';


--
-- Name: COLUMN test_result.tst_rslt_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.tst_rslt_type IS 'Type of parameter: Dictionary, Titer Range, Number Range, Date';


--
-- Name: COLUMN test_result.value; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.value IS 'Possible result value based on type';


--
-- Name: COLUMN test_result.significant_digits; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.significant_digits IS 'Number of decimal digits';


--
-- Name: COLUMN test_result.quant_limit; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.quant_limit IS 'Quantitation Limit (if any)';


--
-- Name: COLUMN test_result.cont_level; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.cont_level IS 'Contamination Level (if any)';


--
-- Name: COLUMN test_result.is_quantifiable; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.is_quantifiable IS 'True if the user should be able to quantify the result.  Is only meaningful for D and M types';


--
-- Name: COLUMN test_result.is_active; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_result.is_active IS 'Is this test_result active.  If a select list option is removed it still may be referenced by a result so we have to inactivate it';


--
-- Name: test_result_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.test_result_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.test_result_seq OWNER TO clinlims;

--
-- Name: test_section; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_section (
    id numeric(10,0) NOT NULL,
    name character varying(20),
    description character varying(60) NOT NULL,
    org_id numeric(10,0),
    is_external character varying(1),
    lastupdated timestamp(6) without time zone,
    parent_test_section numeric(10,0),
    sort_order numeric DEFAULT 2147483647,
    is_active character varying(1) DEFAULT 'Y'::character varying,
    name_localization_id numeric(10,0) NOT NULL,
    display_key character varying(60)
);


ALTER TABLE clinlims.test_section OWNER TO clinlims;

--
-- Name: COLUMN test_section.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_section.name IS 'Short section name';


--
-- Name: COLUMN test_section.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_section.description IS 'MDH Locations including various labs';


--
-- Name: COLUMN test_section.org_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_section.org_id IS 'Sequential Numbering Field';


--
-- Name: COLUMN test_section.is_external; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_section.is_external IS 'Flag indicating if section is external to organization (Y/N)';


--
-- Name: COLUMN test_section.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_section.display_key IS 'Resource file lookup key for localization of displaying the name';


--
-- Name: test_section_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.test_section_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


ALTER TABLE clinlims.test_section_seq OWNER TO clinlims;

--
-- Name: test_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.test_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.test_seq OWNER TO clinlims;

--
-- Name: test_trailer; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_trailer (
    id numeric(10,0) NOT NULL,
    name character varying(20),
    description character varying(60),
    text character varying(4000),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.test_trailer OWNER TO clinlims;

--
-- Name: test_trailer_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.test_trailer_seq
    START WITH 2
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.test_trailer_seq OWNER TO clinlims;

--
-- Name: test_worksheet_item; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_worksheet_item (
    id numeric(10,0) NOT NULL,
    tw_id numeric(10,0) NOT NULL,
    qc_id numeric,
    "position" numeric,
    cell_type character varying(2)
);


ALTER TABLE clinlims.test_worksheet_item OWNER TO clinlims;

--
-- Name: COLUMN test_worksheet_item."position"; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_worksheet_item."position" IS 'Well location or position within the batch';


--
-- Name: COLUMN test_worksheet_item.cell_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_worksheet_item.cell_type IS 'Cell/position type: First, random, duplicate last, last run';


--
-- Name: test_worksheets; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.test_worksheets (
    id numeric(10,0) NOT NULL,
    test_id numeric(10,0),
    batch_capacity numeric,
    total_capacity numeric,
    number_format character varying(1)
);


ALTER TABLE clinlims.test_worksheets OWNER TO clinlims;

--
-- Name: COLUMN test_worksheets.test_id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_worksheets.test_id IS 'Sequential value assigned on insert';


--
-- Name: COLUMN test_worksheets.batch_capacity; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_worksheets.batch_capacity IS 'number of samples (including QC) per batch/plate';


--
-- Name: COLUMN test_worksheets.total_capacity; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_worksheets.total_capacity IS 'Number of samples (including QC) per worksheet';


--
-- Name: COLUMN test_worksheets.number_format; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.test_worksheets.number_format IS 'Specifies the numbering scheme for worksheet cell';


--
-- Name: tobereomved_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.tobereomved_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.tobereomved_seq OWNER TO clinlims;

--
-- Name: type_of_data_indicator; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.type_of_data_indicator (
    id numeric(20,0) NOT NULL,
    name character varying(20) NOT NULL,
    name_key character varying(40),
    description text,
    description_key character varying(40),
    lastupdated timestamp without time zone NOT NULL
);


ALTER TABLE clinlims.type_of_data_indicator OWNER TO clinlims;

--
-- Name: type_of_data_indicator_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.type_of_data_indicator_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.type_of_data_indicator_seq OWNER TO clinlims;

--
-- Name: type_of_provider; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.type_of_provider (
    id numeric(10,0) NOT NULL,
    description character varying(240),
    tp_code character varying(1)
);


ALTER TABLE clinlims.type_of_provider OWNER TO clinlims;

--
-- Name: COLUMN type_of_provider.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_provider.id IS 'A unique auto generated integer number assigned by the database.';


--
-- Name: COLUMN type_of_provider.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_provider.description IS 'Description such as doctor, nurse, clinician, veteranarian.';


--
-- Name: type_of_sample; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.type_of_sample (
    id numeric(10,0) NOT NULL,
    description character varying(40) NOT NULL,
    domain character varying(1),
    lastupdated timestamp(6) without time zone,
    local_abbrev character varying(10),
    is_active boolean DEFAULT true,
    sort_order numeric DEFAULT 2147483647,
    name_localization_id numeric NOT NULL,
    display_key character varying(60)
);


ALTER TABLE clinlims.type_of_sample OWNER TO clinlims;

--
-- Name: COLUMN type_of_sample.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_sample.id IS 'A unique auto generated integer number assigned by the database';


--
-- Name: COLUMN type_of_sample.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_sample.description IS 'Description such as water, tissue, sludge, etc.';


--
-- Name: COLUMN type_of_sample.domain; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_sample.domain IS 'Type of sample this can be applied to : Environ, Animal, Clinical';


--
-- Name: COLUMN type_of_sample.display_key; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_sample.display_key IS 'Resource file lookup key for localization of displaying the name';


--
-- Name: type_of_sample_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.type_of_sample_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.type_of_sample_seq OWNER TO clinlims;

--
-- Name: type_of_test_result; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.type_of_test_result (
    id numeric(10,0) NOT NULL,
    test_result_type character varying(1),
    description character varying(60),
    lastupdated timestamp(6) without time zone,
    hl7_value character varying(20)
);


ALTER TABLE clinlims.type_of_test_result OWNER TO clinlims;

--
-- Name: COLUMN type_of_test_result.id; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_test_result.id IS 'A unique auto generated integer number assigned by database';


--
-- Name: COLUMN type_of_test_result.test_result_type; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_test_result.test_result_type IS 'Test Result Type (T, N, D)';


--
-- Name: COLUMN type_of_test_result.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.type_of_test_result.description IS 'Human readable description';


--
-- Name: type_of_test_result_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.type_of_test_result_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.type_of_test_result_seq OWNER TO clinlims;

--
-- Name: unit_of_measure; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.unit_of_measure (
    id numeric(10,0) NOT NULL,
    name character varying(26),
    description character varying(60),
    lastupdated timestamp(6) without time zone
);


ALTER TABLE clinlims.unit_of_measure OWNER TO clinlims;

--
-- Name: COLUMN unit_of_measure.name; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.unit_of_measure.name IS 'Name of Unit';


--
-- Name: COLUMN unit_of_measure.description; Type: COMMENT; Schema: clinlims; Owner: clinlims
--

COMMENT ON COLUMN clinlims.unit_of_measure.description IS 'Description of Unit';


--
-- Name: unit_of_measure_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.unit_of_measure_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.unit_of_measure_seq OWNER TO clinlims;

--
-- Name: user_alert_map; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.user_alert_map (
    user_id integer NOT NULL,
    alert_id integer,
    report_id integer,
    alert_limit integer,
    alert_operator character varying(255),
    map_id integer NOT NULL
);


ALTER TABLE clinlims.user_alert_map OWNER TO clinlims;

--
-- Name: user_group_map; Type: TABLE; Schema: clinlims; Owner: clinlims
--

CREATE TABLE clinlims.user_group_map (
    user_id integer NOT NULL,
    group_id integer NOT NULL,
    map_id integer NOT NULL
);


ALTER TABLE clinlims.user_group_map OWNER TO clinlims;

--
-- Name: zip_code_seq; Type: SEQUENCE; Schema: clinlims; Owner: clinlims
--

CREATE SEQUENCE clinlims.zip_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE clinlims.zip_code_seq OWNER TO clinlims;

--
-- Name: id; Type: DEFAULT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.data_resource_level_id ALTER COLUMN id SET DEFAULT nextval('clinlims.data_resource_level_id_id_seq'::regclass);


--
-- Data for Name: action; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.action (id, code, description, type, lastupdated) FROM stdin;
1	FSR	Fee sticker received	resolving	2007-08-21 16:48:38.434
2	CDC	Collection date corrected	resolving	2007-08-21 15:58:29.117
3	RQSOC	Request source corrected	resolving	2007-08-21 14:11:01.737
4	SNAC	Submitter name corrected	resolving	2007-08-21 14:21:11.696
26	DLRQR	Delayed request form received	internal	2008-01-11 04:17:09.054
25	CMRE	Communication reviewed	internal	2008-05-01 21:38:41.775
27	DURPS	Duplicate report to submitter	message	2008-01-11 04:22:59.507
28	SPDC	Specimen discarded	internal	2008-01-11 04:20:13.235
29	SCL	Submitter was called	message	2008-01-11 04:22:33.637
30	SPSOC	Specimen source corrected	internal	2008-01-11 04:24:33.863
31	DLRQRQ	Delayed request form requested	message	2008-01-11 04:46:58.057
32	RPDF	Report placed in dead file	internal	2008-01-11 04:47:25.498
33	RQIDC	Request form ID corrected	internal	2008-01-11 04:47:57.136
34	SPCA	Specimen canceled	internal	2008-01-11 04:48:25.614
35	SPIDC	Specimen ID corrected	internal	2008-01-11 04:48:47.451
36	SPUNS	Specimen declared unsatisfactory	internal	2008-01-11 04:49:13.262
\.


--
-- Name: action_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.action_seq', 45, false);


--
-- Data for Name: address_part; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.address_part (id, part_name, display_order, display_key) FROM stdin;
1	department	\N	address.department
2	commune	\N	address.commune
3	village	\N	address.village
4	fax	\N	address.fax
5	phone	\N	address.phone
6	street	\N	address.street
\.


--
-- Name: address_part_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.address_part_seq', 6, true);


--
-- Data for Name: analysis; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.analysis (id, sampitem_id, test_sect_id, test_id, revision, status, started_date, completed_date, released_date, printed_date, is_reportable, so_send_ready_date, so_client_reference, so_notify_received_date, so_notify_send_date, so_send_date, so_send_entry_by, so_send_entry_date, analysis_type, lastupdated, parent_analysis_id, parent_result_id, reflex_trigger, status_id, entry_date, panel_id, referred_out, type_of_sample_name, corrected) FROM stdin;
\.


--
-- Data for Name: analysis_qaevent; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.analysis_qaevent (id, qa_event_id, analysis_id, lastupdated, completed_date) FROM stdin;
\.


--
-- Data for Name: analysis_qaevent_action; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.analysis_qaevent_action (id, analysis_qaevent_id, action_id, created_date, lastupdated, sys_user_id) FROM stdin;
\.


--
-- Name: analysis_qaevent_action_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.analysis_qaevent_action_seq', 221, false);


--
-- Name: analysis_qaevent_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.analysis_qaevent_seq', 326, false);


--
-- Name: analysis_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.analysis_seq', 1, false);


--
-- Data for Name: analyte; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.analyte (id, analyte_id, name, is_active, external_id, lastupdated, local_abbrev) FROM stdin;
44	\N	HIV-2 Result	Y	\N	2007-05-15 13:29:44.989	\N
62	\N	MEP Agar	Y	\N	2007-10-09 08:38:25.01	\N
63	\N	MYP Agar	Y	\N	2007-10-09 08:38:31.459	\N
64	\N	PLET Agar	Y	\N	2007-10-09 08:38:41.299	\N
65	\N	SBA Agar	Y	\N	2007-10-09 08:38:49.58	\N
67	\N	Extraction Kit	Y	\N	2007-12-10 09:04:09.124	\N
68	\N	Amplification Kit	Y	\N	2007-12-10 09:04:16.37	\N
69	\N	Light Cycler	Y	\N	2007-12-10 09:04:24.964	\N
70	\N	ABI 7000	Y	\N	2007-12-10 09:04:36.709	\N
71	\N	i Cycler	Y	\N	2007-12-10 09:04:56.821	\N
72	\N	ABI 7500	Y	\N	2007-12-10 09:05:04.01	\N
73	\N	DFA Capsule Antigen Detection	Y	\N	2007-12-10 09:05:41.24	\N
74	\N	DFA Cell Wall Detection	Y	\N	2007-12-10 09:06:56.313	\N
78	\N	Dilution 1:10	N	\N	2007-12-27 03:05:58.517	\N
89	\N	TRF Spore Detection Interpretation	Y	\N	2007-12-27 05:34:16.557	\N
90	\N	TRF Cell Detection Data Value	Y	\N	2007-12-27 05:34:57.18	\N
91	\N	TRF Cell Detection Result	Y	\N	2007-12-27 05:35:11.751	\N
92	\N	TRF Cell Detection Interpretation	Y	\N	2007-12-27 05:35:25.667	\N
1	\N	Influenza Virus B RNA	Y	\N	2007-02-02 14:09:33.524	\N
45	\N	Final HIV Interpretation 	Y	\N	2007-05-15 13:30:15.23	\N
47	\N	Penicillin	Y	\N	2007-05-18 09:41:04.107	\N
48	\N	Ceftriaxone	Y	\N	2007-05-18 09:41:14.295	\N
49	\N	Ciprofloxacin 	Y	\N	2007-05-18 09:43:17.764	\N
50	\N	Spectinomycin	Y	\N	2007-05-18 09:43:30.214	\N
43	\N	HIV-1 Result	Y	\N	2007-05-15 13:29:33.206	\N
51	\N	Tetracycline	Y	\N	2007-05-18 09:43:37.86	\N
52	\N	Cefixime	Y	\N	2007-05-18 09:43:52.144	\N
53	\N	Azithromycin	Y	\N	2007-05-18 09:44:01.789	\N
15	\N	Isoniazid 0.4 mcg.ml	Y	\N	2007-02-13 14:30:20.194	\N
16	\N	Streptomycin 2.0 mcg/ml	Y	\N	2007-02-13 14:30:43.17	\N
17	\N	Ethambutol 2.5 mcg/ml	Y	\N	2007-02-13 14:30:58.832	\N
18	\N	Pyrazinamide 100 mcg/ml	Y	\N	2007-02-13 14:32:46.073	\N
19	\N	DFA Capsule Antigen Detection from Isolate	Y	\N	2007-12-11 03:56:08.454	\N
8	\N	Respiratory Syncytial Virus A RNA	Y	\N	2007-02-02 14:13:44.532	\N
9	\N	Respiratory Syncytial Virus B RNA	Y	\N	2007-02-02 14:13:56.705	\N
20	\N	DFA Cell Wall Detection from Isolate	Y	\N	2007-12-11 03:56:47.987	\N
38	\N	Comment 8	Y	\N	2007-04-02 09:05:17.216	\N
41	\N	Titer	Y	\N	2007-05-03 15:59:07.522	\N
22	\N	Preliminary Result	Y	\N	2007-03-28 14:00:45.933	\N
23	\N	Result 4	Y	\N	2007-03-28 14:00:51.064	\N
24	\N	Result 5	Y	\N	2007-03-28 14:01:00.965	\N
25	\N	Result 6	Y	\N	2007-03-28 14:01:05.352	\N
26	\N	Result 7	Y	\N	2007-03-28 14:01:16.014	\N
27	\N	Result 8	Y	\N	2007-03-28 14:01:20.594	\N
28	\N	Result 9	Y	\N	2007-03-28 14:01:31.066	\N
29	\N	Preliminary Result Modifier	Y	\N	2007-03-28 16:22:57.579	\N
30	\N	Final Result Modifier	Y	\N	2007-03-28 16:22:21.34	\N
208	\N	Influenza Virus A/H3 RNA	Y	\N	2006-09-07 08:32:05	\N
209	\N	Influenza Virus A/H5 RNA	Y	\N	2006-09-07 08:32:21	\N
210	\N	Influenza Virus A/H7 RNA	Y	\N	2006-10-13 10:34:13.207	\N
211	\N	Influenza Virus A/H9 RNA	Y	\N	2008-01-23 08:02:52.021	\N
212	\N	Final Result	Y	\N	2006-09-07 08:34:31	\N
213	\N	Presumptive Result	Y	\N	2006-09-07 08:34:40	\N
214	\N	Result 1	Y	\N	2006-11-07 08:11:16	\N
215	\N	Result 2	Y	\N	2006-10-20 13:28:24	\N
216	\N	Result 3	Y	\N	2006-10-20 13:28:30	\N
217	\N	Interpretation	Y	\N	2006-09-07 08:35:29	\N
21	\N	Comment	Y	\N	2007-02-27 11:09:27.335	\N
54	\N	Gentamycin Interpretation	Y	\N	2007-06-20 10:01:45.134	\N
220	\N	BAND GP160	Y	\N	2006-10-18 09:19:37.724	\N
221	\N	BAND GP120	Y	\N	2006-10-18 09:19:38.505	\N
222	\N	BAND P65	Y	\N	2006-09-07 08:39:02	\N
223	\N	BAND P55	Y	\N	2006-09-07 08:39:12	\N
224	\N	BAND P51	Y	\N	2006-09-07 08:39:22	\N
225	\N	BAND GP41	Y	\N	2006-10-18 09:19:37.302	\N
226	\N	BAND P40	Y	\N	2006-11-07 13:55:36.669	\N
227	\N	BAND P31	Y	\N	2006-11-06 13:15:38.449	\N
228	\N	BAND P24	Y	\N	2006-11-06 13:15:29.222	\N
229	\N	BAND P18	Y	\N	2006-11-08 08:09:44.805	\N
246	\N	Rnase P Interpretation	Y	\N	2006-09-18 10:27:21	\N
251	\N	Result Status	Y	\N	2006-10-10 09:27:45	\N
252	\N	Probability	Y	\N	2006-10-03 10:24:02	\N
271	\N	Modifier 1	Y	\N	2006-10-20 09:28:32	\N
272	\N	Modifier 2	Y	\N	2006-10-20 09:28:37	\N
273	\N	Modifier 3	Y	\N	2006-10-20 09:28:42	\N
274	\N	Result Status 1	Y	\N	2006-10-20 09:28:51	\N
275	\N	Result Status 2	Y	\N	2006-10-20 09:28:59	\N
276	\N	Result Status 3	Y	\N	2006-10-20 09:29:06	\N
206	\N	Influenza Virus A RNA	Y	\N	2007-02-02 14:19:32.675	\N
207	\N	Influenza Virus A/H1 RNA	Y	\N	2006-09-07 08:31:50	\N
240	\N	BA2 CT value	Y	\N	2006-10-18 09:19:43.005	\N
244	\N	Extraction Method	Y	\N	2006-09-20 16:26:23	\N
245	\N	16S Interpretation	Y	\N	2006-10-25 11:21:09.457	\N
40	\N	Agent	Y	\N	2007-04-12 09:42:50.799	\N
249	\N	Modifier	Y	\N	2006-10-02 10:23:08	\N
250	\N	Quantity	Y	\N	2006-10-03 09:12:43	\N
253	\N	Method	Y	\N	2006-11-08 10:28:43	\N
266	\N	Capsule M-Fadyean	Y	\N	2006-10-11 13:51:30	\N
234	\N	BA3 interpretation	Y	\N	2006-10-18 09:19:39.302	\N
235	\N	16S CT value	Y	\N	2007-06-18 08:49:51.801	\N
232	\N	BA1 interpretation	Y	\N	2006-10-18 09:19:44.442	\N
233	\N	BA2 interpretation	Y	\N	2006-10-18 09:19:42.067	\N
236	\N	Rnase P CT value	Y	\N	2006-10-13 09:57:57.558	\N
237	\N	Result	Y	\N	2006-09-18 10:03:34	\N
46	\N	Western Blot Interpretation	Y	\N	2007-05-15 13:48:03.305	\N
239	\N	BA1 CT value	Y	\N	2006-10-23 14:12:56.284	\N
241	\N	BA3 CT value	Y	\N	2006-10-18 09:19:40.583	\N
242	\N	TRF Spore Detection Dilution	Y	\N	2007-12-27 05:33:28.883	\N
243	\N	TRF Cell Detection Dilution	Y	\N	2007-12-27 05:34:38.238	\N
247	\N	Disclaimer	Y	\N	2006-09-20 16:26:06	\N
256	\N	Rifampin 2.0 mcg/ml	Y	\N	2006-10-10 09:29:37	\N
55	\N	Kanamycin Interpretation	Y	\N	2007-06-20 10:02:26.74	\N
255	\N	Rifampin 1.0 mcg/ml	Y	\N	2006-10-10 09:29:21	\N
257	\N	Isoniazid 0.1 mcg/ml	Y	\N	2006-10-10 09:29:54	\N
258	\N	Colony Morphology	Y	\N	2006-10-11 13:49:58	\N
259	\N	Hemolysis	Y	\N	2006-10-11 13:50:05	\N
260	\N	Gram stain	Y	\N	2006-10-11 13:50:12	\N
261	\N	Motility wet mount	Y	\N	2006-10-11 13:50:35	\N
262	\N	Gamma phage	Y	\N	2006-10-11 13:50:43	\N
263	\N	DFA capsule from specimen	N	\N	2007-12-10 09:06:14.825	\N
264	\N	DFA cell wall from specimen	N	\N	2007-12-10 09:06:14.846	\N
265	\N	Capsule India Ink	Y	\N	2006-10-11 13:51:06	\N
267	\N	Capsule bicarbonate	Y	\N	2006-10-11 13:51:40	\N
268	\N	Catalase	Y	\N	2006-10-11 13:51:46	\N
269	\N	Malachite green for spores	Y	\N	2006-10-11 13:51:57	\N
270	\N	Wet mount for spores	Y	\N	2006-10-11 13:52:38	\N
31	\N	Comment 1	Y	\N	2007-04-02 09:04:22.529	\N
32	\N	Comment 2	Y	\N	2007-04-02 09:04:28.801	\N
33	\N	Comment 3	Y	\N	2007-04-02 09:04:41.088	\N
34	\N	Comment 4	Y	\N	2007-04-02 09:04:46.92	\N
35	\N	Comment 5	Y	\N	2007-04-02 09:04:56.112	\N
36	\N	Comment 6	Y	\N	2007-04-02 09:05:01.84	\N
37	\N	Comment 7	Y	\N	2007-04-02 09:05:08.789	\N
39	\N	Comment 9	Y	\N	2007-04-02 09:05:21.5	\N
56	\N	Gentamycin	Y	\N	2007-06-20 10:02:52.199	\N
57	\N	Kanamycin	Y	\N	2007-06-20 10:03:02.449	\N
58	\N	Motility Standard Media	Y	\N	2007-10-02 08:38:33.138	\N
59	\N	Standard Motility Media	Y	\N	2007-10-09 08:37:56.727	\N
60	\N	Chocolate Agar	Y	\N	2007-10-09 08:38:11.08	\N
61	\N	DEA Agar	Y	\N	2007-10-09 08:38:17.659	\N
66	\N	Test Moiety	Y	\N	2007-12-10 09:03:38.415	\N
75	\N	Choose Equipment	Y	\N	2007-12-11 05:35:57.035	\N
76	\N	Previous FTA Reactivity	Y	\N	2007-12-27 02:40:54.886	\N
77	\N	Fluorescence Grading	Y	\N	2007-12-27 02:41:18.215	\N
79	\N	E. coli 25922	Y	\N	2007-12-27 03:34:33.647	\N
80	\N	P. aeruginosa	Y	\N	2007-12-27 03:39:41.842	\N
81	\N	S. aureus	Y	\N	2007-12-27 03:34:57.55	\N
82	\N	Susceptible	N	\N	2007-12-27 03:41:45.362	\N
83	\N	Nonsusceptible - Contact CDC for confirmation of resistance	N	\N	2007-12-27 03:41:30.012	\N
84	\N	Test Not Performed	N	\N	2007-12-27 03:41:50.671	\N
85	\N	No Pass	Y	\N	2007-12-27 03:36:31.375	\N
86	\N	Pass	Y	\N	2007-12-27 03:36:35.561	\N
87	\N	TRF Spore Detection Date Value	Y	\N	2007-12-27 05:33:47.18	\N
88	\N	TRF Spore Detection Result	Y	\N	2007-12-27 05:34:05.441	\N
93	\N	test kit	Y		2009-04-03 10:15:45.64	TESTKIT
94	\N	Conclusion	Y	\N	2010-10-28 06:12:42.031482	\N
95	\N	generated CD4 Count	Y	\N	2010-10-28 06:13:55.508252	\N
96	\N	VIH Test - Collodial Gold/Shangai Kehua Result	Y	\N	2011-02-02 11:55:53.383208	\N
97	\N	Determine Result	Y	\N	2011-02-02 11:55:53.383208	\N
98	\N	Determine Results	Y	\N	2013-10-10 16:53:00.019523	\N
99	\N	Genie III Result	Y	\N	2013-10-10 16:53:00.019523	\N
111	\N	p24 Ag Result	Y	\N	2020-01-22 21:46:41.931967	\N
112	\N	Western Blot 2 Result	Y	\N	2020-01-22 21:46:41.931967	\N
113	\N	Western Blot 1 Result	Y	\N	2020-01-22 21:46:41.931967	\N
114	\N	Genie II 10 Result	Y	\N	2020-01-22 21:46:41.931967	\N
115	\N	Genie II 100 Result	Y	\N	2020-01-22 21:46:41.931967	\N
116	\N	Genie II Result	Y	\N	2020-01-22 21:46:41.931967	\N
100	\N	Vironostika Result	Y	\N	2020-01-22 21:46:41.931967	\N
101	\N	Murex Result	Y	\N	2020-01-22 21:46:41.931967	\N
102	\N	Integral Result	Y	\N	2020-01-22 21:46:41.931967	\N
103	\N	CD4 percentage count Result	Y	\N	2020-01-22 21:46:41.931967	\N
104	\N	GB Result	Y	\N	2020-01-22 21:46:41.931967	\N
105	\N	Lymph % Result	Y	\N	2020-01-22 21:46:41.931967	\N
106	\N	Conclusion	Y	\N	2020-01-22 21:46:41.931967	\N
107	\N	generated CD4 Count	Y	\N	2020-01-22 21:46:41.931967	\N
108	\N	Bioline Results	Y	\N	2020-01-22 21:46:41.931967	\N
109	\N	Innolia Results	Y	\N	2020-01-22 21:46:41.931967	\N
110	\N	Vironostika Unreflex Result	Y	\N	2020-01-22 21:46:41.931967	\N
\.


--
-- Name: analyte_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.analyte_seq', 99, true);


--
-- Data for Name: analyzer; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.analyzer (id, scrip_id, name, machine_id, description, analyzer_type, is_active, location, lastupdated) FROM stdin;
1	\N	sysmex	1	bootstrap machine	\N	t	\N	2009-11-25 15:35:31.343118
3	\N	cobas_integra	\N	cobas_integra	\N	t	\N	2009-12-14 15:35:31.34118
\.


--
-- Data for Name: analyzer_result_status; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.analyzer_result_status (id, name, description) FROM stdin;
1	NOT_REVIEWED	The result has not yet been reviewed by the user
2	ACCEPTED	The result has been reviewed and accepted by the user
3	DECLINED	The result has been reviewed and not accepted by the user
4	MATCHING_ACCESSION_NOT_FOUND	The Lab No does not exist in the system
5	MATCHING_TEST_NOT_FOUND	The Lab No exists but the test has not been entered
6	TEST_MAPPING_NOT_FOUND	The test name from the analyzer is not recognized
7	ERROR	The result sent from the analyzer can not be understood
\.


--
-- Data for Name: analyzer_results; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.analyzer_results (id, analyzer_id, accession_number, test_name, result, units, status_id, iscontrol, lastupdated, read_only, duplicate_id, positive, complete_date, test_result_type, test_id) FROM stdin;
\.


--
-- Name: analyzer_results_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.analyzer_results_seq', 1, false);


--
-- Name: analyzer_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.analyzer_seq', 1, true);


--
-- Data for Name: analyzer_test_map; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.analyzer_test_map (analyzer_id, analyzer_test_name, test_id, lastupdated) FROM stdin;
\.


--
-- Data for Name: attachment; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.attachment (id, attach_type, filename, description, storage_reference) FROM stdin;
\.


--
-- Data for Name: attachment_item; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.attachment_item (id, reference_id, reference_table_id, attachment_id) FROM stdin;
\.


--
-- Data for Name: barcode_label_info; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.barcode_label_info (id, code, numprinted, type, lastupdated) FROM stdin;
\.


--
-- Name: barcode_label_info_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.barcode_label_info_seq', 1, false);


--
-- Name: city_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.city_seq', 1, false);


--
-- Data for Name: city_state_zip; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.city_state_zip (id, city, state, zip_code, county_fips, county, region_id, region, state_fips, state_name, lastupdated) FROM stdin;
78708	BROADWAY	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78709	CAPITOL HILL	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78711	GREENWOOD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78713	WALLINGFORD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78715	POINEER SQUARE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78717	SEATTLE	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78719	SEATTLE	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78723	SEATTLE	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78725	QUEEN ANNE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78707	TIMES SQUARE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78710	SEATTLE	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78712	SEATTLE	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78714	INTERNATIONAL	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78716	SEATTLE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78718	UNIVERSITY	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78720	WHITE CENTER	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78722	SEATTLE	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78724	TUKWILA	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78726	SEATTLE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78706	SEATTLE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78721	BALLARD	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78708	BROADWAY	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78709	CAPITOL HILL	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78711	GREENWOOD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78713	WALLINGFORD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78715	POINEER SQUARE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78717	SEATTLE	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78719	SEATTLE	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78723	SEATTLE	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78725	QUEEN ANNE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78707	TIMES SQUARE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78710	SEATTLE	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78712	SEATTLE	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78714	INTERNATIONAL	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78716	SEATTLE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78718	UNIVERSITY	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78720	WHITE CENTER	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78722	SEATTLE	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78724	TUKWILA	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78726	SEATTLE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78706	SEATTLE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78721	BALLARD	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78708	BROADWAY	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78709	CAPITOL HILL	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78711	GREENWOOD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78713	WALLINGFORD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78715	POINEER SQUARE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78717	SEATTLE	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78719	SEATTLE	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78723	SEATTLE	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78725	QUEEN ANNE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78707	TIMES SQUARE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78710	SEATTLE	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78712	SEATTLE	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78714	INTERNATIONAL	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78716	SEATTLE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78718	UNIVERSITY	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78720	WHITE CENTER	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78722	SEATTLE	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78724	TUKWILA	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78726	SEATTLE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78706	SEATTLE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78721	BALLARD	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78708	BROADWAY	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78709	CAPITOL HILL	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78711	GREENWOOD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78713	WALLINGFORD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78715	POINEER SQUARE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78717	SEATTLE	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78719	SEATTLE	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78723	SEATTLE	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78725	QUEEN ANNE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78707	TIMES SQUARE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78710	SEATTLE	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78712	SEATTLE	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78714	INTERNATIONAL	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78716	SEATTLE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78718	UNIVERSITY	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78720	WHITE CENTER	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78722	SEATTLE	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78724	TUKWILA	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78726	SEATTLE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78706	SEATTLE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78721	BALLARD	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78708	BROADWAY	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78709	CAPITOL HILL	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78711	GREENWOOD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78713	WALLINGFORD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78715	POINEER SQUARE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78717	SEATTLE	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78719	SEATTLE	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78723	SEATTLE	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78725	QUEEN ANNE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78707	TIMES SQUARE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78710	SEATTLE	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78712	SEATTLE	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78714	INTERNATIONAL	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78716	SEATTLE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78718	UNIVERSITY	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78720	WHITE CENTER	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78722	SEATTLE	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78724	TUKWILA	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78726	SEATTLE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78706	SEATTLE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78721	BALLARD	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78708	BROADWAY	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78709	CAPITOL HILL	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78711	GREENWOOD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78713	WALLINGFORD	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78715	POINEER SQUARE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78717	SEATTLE	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78719	SEATTLE	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78723	SEATTLE	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78725	QUEEN ANNE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78707	TIMES SQUARE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78710	SEATTLE	WA	98102	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78712	SEATTLE	WA	98103	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78714	INTERNATIONAL	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78716	SEATTLE	WA	98104	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78718	UNIVERSITY	WA	98105	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78720	WHITE CENTER	WA	98106	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78722	SEATTLE	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78724	TUKWILA	WA	98108	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78726	SEATTLE	WA	98109	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78706	SEATTLE	WA	98101	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
78721	BALLARD	WA	98107	33	KING	\N	\N	53	WASHINGTON          	2006-12-04 12:46:48
\.


--
-- Data for Name: code_element_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.code_element_type (id, text, lastupdated, local_reference_table) FROM stdin;
1	TEST	2007-03-07 15:27:58.72	5
2	STATUS OF SAMPLE	2007-03-07 15:28:22.718	40
\.


--
-- Name: code_element_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.code_element_type_seq', 21, false);


--
-- Data for Name: code_element_xref; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.code_element_xref (id, message_org_id, code_element_type_id, receiver_code_element_id, local_code_element_id, lastupdated) FROM stdin;
1	22	1	1	68	2007-03-07 15:37:04.13
2	22	1	2	67	2007-03-07 15:37:31.067
4	22	1	3	49	2007-03-07 15:39:40.635
\.


--
-- Name: code_element_xref_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.code_element_xref_seq', 41, false);


--
-- Name: county_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.county_seq', 1, false);


--
-- Data for Name: data_indicator; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.data_indicator (id, data_value_id, year, month, type_of_indicator_id, status, lastupdated) FROM stdin;
\.


--
-- Name: data_indicator_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.data_indicator_seq', 1, false);


--
-- Data for Name: data_resource; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.data_resource (id, name, collection_name, header_key, level, indicator_id, lastupdated) FROM stdin;
\.


--
-- Data for Name: data_resource_level_id; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.data_resource_level_id (id, level, id_for_level, data_resource_id) FROM stdin;
\.


--
-- Name: data_resource_level_id_id_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.data_resource_level_id_id_seq', 1, false);


--
-- Name: data_resource_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.data_resource_seq', 1, false);


--
-- Data for Name: data_value; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.data_value (id, data_resource_id, index, column_name, value, display_key, visible, lastupdated) FROM stdin;
\.


--
-- Name: data_value_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.data_value_seq', 1, false);


--
-- Data for Name: dictionary; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.dictionary (id, is_active, dict_entry, lastupdated, local_abbrev, dictionary_category_id, display_key, sort_order, name_localization_id) FROM stdin;
2	Y	INFLUENZA VIRUS A/H1 RNA DETECTED	2007-10-11 08:46:31.254	\N	3	\N	200	\N
3	Y	INFLUENZA VIRUS A/H5 RNA DETECTED	2007-10-11 08:46:52.81	\N	3	\N	300	\N
273	N	ABI 7000	2009-07-31 10:03:29.609	\N	3	\N	27300	\N
520	Y	Pateuse	2009-12-04 13:21:26.574	PATEUSE	38	dictionary.result.Pateuse	52000	\N
600	Y	GP160	2010-01-12 13:51:53.513	GP160	38	dictionary.result.GP160	60000	\N
601	Y	P66	2010-01-12 13:52:41.926	P66	38	dictionary.result.P66	60100	\N
640	Y	P55	2010-01-12 13:52:58.472	P55	38	dictionary.result.P55	64000	\N
37	Y	FINAL INTERPRETATION: These results most closely indicate the presence of HIV-2 antibodies. Confirmation testing for HIV-2 will be performed at the Centers for Disease Control and Prevention on this current specimen. Please submit another specimen one month following the collection of the first specimen for repeat testing. 	2007-05-07 14:27:13	H25	3	\N	3700	\N
86	Y	NEGATIVE (NOT LYSED)	2006-09-20 16:30:44	\N	3	\N	8600	\N
87	Y	POSITIVE (LYSED)	2006-09-20 16:31:19	\N	3	\N	8700	\N
88	Y	No Spores Present	2007-10-09 13:29:52.416	\N	3	\N	8800	\N
102	Y	acid fast bacilli	2006-10-16 15:03:56.035	\N	3	\N	10200	\N
103	Y	PRESENT	2006-10-02 13:57:39	\N	3	\N	10300	\N
105	Y	Mycobacterium not suggestive of M tuberculosis	2006-10-02 14:04:44	\N	3	\N	10500	\N
106	Y	Mycobacterium fortuitum	2006-10-02 14:04:56	\N	3	\N	10600	\N
107	Y	Mycobacterium kansasii	2006-10-02 14:05:18	\N	3	\N	10700	\N
108	Y	Mycobacterium species	2006-10-02 14:05:35	\N	3	\N	10800	\N
111	Y	1+ (rare)	2006-10-10 10:47:41	\N	3	\N	11100	\N
112	Y	2+ (few)	2006-10-10 10:47:43	\N	3	\N	11200	\N
113	Y	3+ (moderate)	2006-10-10 10:47:44	\N	3	\N	11300	\N
114	Y	4+ (numerous)	2006-10-10 10:47:46	\N	3	\N	11400	\N
119	Y	PENDING	2006-10-03 09:25:55	\N	3	\N	11900	\N
90	Y	DETECTED	2006-09-20 16:31:57	\N	3	\N	9000	\N
38	Y	FINAL INTERPRETATION: These results most closely indicate the presence of HIV antibodies (without differentiation). Please submit another specimen one month following the first specimen for repeat testing. This subsequent specimen may allow for HIV antibody confirmation and differentiation. 	2007-05-07 14:27:13	H26	3	\N	3800	\N
92	Y	Presumptive for Bacillus Anthracis	2007-10-09 13:33:16.867	\N	3	\N	9200	\N
93	Y	Bacillus anthracis	2006-10-11 14:24:46	\N	3	\N	9300	\N
99	N	FLUORESCENCE NOT DETECTED	2007-12-10 08:48:42.818	\N	3	\N	9900	\N
100	Y	NOT TESTED	2006-09-20 16:34:09	\N	3	\N	10000	\N
101	Y	WARNING: PCR results are FOR RESEARCH USE ONLY and should not be used for diagnosis of disease. Patient management decisions must be based on appropriate findings by a physician and the clinical condition of the patient.	2006-10-16 15:04:01.132	\N	3	\N	10100	\N
109	Y	Mycobacterium tuberculosis complex	2006-10-02 14:05:58	\N	3	\N	10900	\N
110	Y	Mycobacterium gordonae	2006-10-02 14:06:11	\N	3	\N	11000	\N
116	Y	RESISTANT	2006-10-03 09:25:25	\N	3	\N	11600	\N
117	Y	UNINTERPRETABLE	2006-10-03 09:25:45	\N	3	\N	11700	\N
124	Y	Most Closely Resembles	2007-10-10 12:44:36.813	\N	3	\N	12400	\N
125	Y	PRESUMPTIVE	2006-10-10 09:21:47	\N	3	\N	12500	\N
143	Y	-	2006-11-01 10:59:32.624	\N	3	\N	14300	\N
56	Y	Neisseria gonorrhoeae not found 	2007-05-15 14:10:32.538	\N	3	\N	5600	\N
57	Y	IMPORTANT: You are legally required to return a case report form. See enclosures.	2007-05-15 14:12:02.896	\N	3	\N	5700	\N
122	Y	acid fast bacilli present	2006-10-10 09:08:55	\N	3	\N	12200	\N
123	Y	acid fast bacilli not found	2006-10-10 09:09:07	\N	3	\N	12300	\N
141	Y	+/-	2006-11-01 10:16:32.717	\N	3	\N	14100	\N
149	Y	REDUCED SUSCEPTIBILITY	2007-10-09 13:33:42.967	\N	3	\N	14900	\N
65	Y	INTERMEDIATE	2007-05-18 13:35:55.083	\N	3	\N	6500	\N
51	Y	REPEATEDLY REACTIVE	2007-05-11 15:35:34.997	\N	3	\N	5100	\N
53	Y	Neisseria gonorrhoeae present	2007-05-15 14:08:07.576	\N	3	\N	5300	\N
150	Y	Weakly Reactive	2007-10-09 13:36:48.001	\N	3	\N	15000	\N
169	Y	No interpretation	2007-06-15 15:56:56.402	NI	3	\N	16900	\N
209	Y	<2	2007-06-20 09:59:13.276	<2	3	\N	20900	\N
210	Y	4	2007-06-20 09:59:23.908	4	3	\N	21000	\N
211	Y	8	2007-06-20 09:59:33.83	8	3	\N	21100	\N
212	Y	>16	2007-06-20 09:59:48.261	>16	3	\N	21200	\N
249	Y	No growth present	2007-10-08 09:55:01.109	\N	3	\N	24900	\N
250	Y	Motile	2007-10-08 09:55:32.44	\N	3	\N	25000	\N
251	Y	Motility test inconclusive	2007-10-08 09:55:53.953	\N	3	\N	25100	\N
265	Y	Fluorescence detected	2007-12-27 03:07:17.178	\N	3	\N	26500	\N
266	Y	Bacillus anthracis DNA detected	2007-10-08 10:05:39.554	\N	3	\N	26600	\N
267	Y	Inconclusive for Bacillus anthracis DNA	2007-10-08 10:06:07.064	\N	3	\N	26700	\N
268	Y	No Bacillus anthracis DNA detected	2007-10-08 10:06:30.659	\N	3	\N	26800	\N
269	Y	Enrichment	2007-10-08 10:07:02.446	\N	3	\N	26900	\N
270	Y	Recovered isolate	2007-10-08 10:07:36.743	\N	3	\N	27000	\N
271	Y	Sample wash or extract	2007-10-08 10:09:14.495	\N	3	\N	27100	\N
272	Y	Light cycler	2007-10-08 10:09:31.913	\N	3	\N	27200	\N
274	Y	I cycler	2007-10-08 10:09:52.084	\N	3	\N	27400	\N
275	Y	Cell lysates	2007-10-08 10:12:28.743	\N	3	\N	27500	\N
276	Y	MasterPure complete RNA and DNA purification	2007-10-08 10:19:21.954	\N	3	\N	27600	\N
277	Y	Spore prep	2007-10-08 10:19:44.624	\N	3	\N	27700	\N
278	Y	Roche Fast Start DNA Master Hyridization Probe Kit	2007-10-08 10:25:54.483	\N	3	\N	27800	\N
306	Y	Variable: Bacilli in no discernable pattern	2007-10-08 07:56:05.985	\N	3	\N	30600	\N
307	Y	Variable: Bacilli in clusters	2007-10-08 07:56:21.999	\N	3	\N	30700	\N
309	Y	Variable: Cocci in chains	2007-10-08 07:57:53.608	\N	3	\N	30900	\N
310	Y	Variable: Cocci in no discernable pattern	2007-10-08 07:58:14.118	\N	3	\N	31000	\N
311	Y	Variable: Cocci in clusters	2007-10-08 07:58:29.634	\N	3	\N	31100	\N
312	Y	Variable: Cocci in pairs	2007-10-08 07:59:02.003	\N	3	\N	31200	\N
313	Y	Variable: Coccobacilli in chains	2007-10-08 07:59:25.137	\N	3	\N	31300	\N
334	Y	Internal	2007-10-25 10:58:02	\N	36	\N	33400	\N
335	Y	Warning	2007-10-25 10:58:06	\N	36	\N	33500	\N
16	Y	Unsat interpretation 	2007-04-24 14:16:26.355	\N	3	\N	1600	\N
17	Y	UNINTERPRETABLE = Bands on Western Blot suggest possible HIV vaccine recipient.	2007-05-07 14:23:44	H4	3	\N	1700	\N
18	Y	POSITIVE = Specific cytoplasmic staining present. Patient is positive for anti-HIV-1 antibody.	2007-05-07 14:23:44	H5	3	\N	1800	\N
19	Y	NEGATIVE = No specific fluorescent staining present. Patient is negative for HIV-1 antibody.	2007-05-07 14:23:44	H6	3	\N	1900	\N
20	Y	INDETERMINATE = Non-specific staining present. Please submit another specimen.	2007-05-07 14:23:44	H7	3	\N	2000	\N
21	Y	NONREACTIVE = No anti-HIV-1 antibody detected.	2007-05-07 14:23:44	H0	3	\N	2100	\N
22	Y	POSITIVE = Any two of the following bands present: p24, gp41, and gp120/160. Other viral bands may be present. Indicates patient is positive for anti-HIV-1 antibody.	2007-05-07 14:23:44	H1	3	\N	2200	\N
23	Y	NEGATIVE = No bands present indicating absence of anti-HIV-1 antibody. Please submit another OraSure (or serum) sample one month following the first sample for repeat testing.	2007-05-07 14:23:44	H2	3	\N	2300	\N
24	Y	INDETERMINATE = Band pattern does not meet criteria for positive. Repeatedly similar indeterminate patterns indicate bands not specific for HIV. Please submit another OraSure (or serum) sample on month following the first sample for repeat testing.	2007-05-07 14:23:44	H3	3	\N	2400	\N
1	Y	INFLUENZA VIRUS A RNA DETECTED	2007-10-11 08:46:19.447	\N	3	\N	100	\N
4	Y	INFLUENZA VIRUS A/H3 RNA DETECTED	2007-10-11 08:46:42.949	\N	3	\N	400	\N
25	Y	One or more bands are present but the blot does not meet the criteria for a positive HIV-1 Western Blot result. NOTE: The HIV-1 Western Blot confirms the presence of antibodies to HIV. This test does not always distinguish between the antibodies to HIV-1 and HIV-2 due to cross reactivity between the antibodies. If you suspect HIV-2, please contact the MDH Epidemiology at 877-676-5414 or 651-201-5414.	2007-05-07 14:23:44	H13	3	\N	2500	\N
26	Y	No bands present on the HIV-1 Western Blot. NOTE:The HIV-1 Western Blot confirms the presence of antibodies to HIV. This test does not always distinguish between the antibodies to HIV-1 and HIV-2 due to cross reactivity between the antibodies. If you suspect HIV-2, please contact the MDH Epidemiology at 877-676-5414 or 651-201-5414.	2007-05-07 14:23:44	H14	3	\N	2600	\N
27	Y	The HIV-1 Western Blot confirms the presence of antibodies to HIV. This test does not always distinguish between the antibodies to HIV-1 and HIV-2 due to cross reactivity between the antibodies. If you suspect HIV-2, please contact the MDH Epidemiology at 877-676-5414 or 651-201-5414.	2007-05-07 14:23:44	H12	3	\N	2700	\N
28	Y	PRELIMINARY POSITIVE screen for HIV-1 antibodies and PRELIMINARY NEGATIVE screen for HIV-2 antibodies	2007-05-07 14:23:44	H8	3	\N	2800	\N
29	Y	PRELIMINARY NEGATIVE screen for HIV-1 antibodies and PRELIMINARY POSITIVE screen for HIV-2 antibodies	2007-05-07 14:23:44	H9	3	\N	2900	\N
30	Y	PRELIMINARY POSITIVE screen for antibodies to HIV-1 and HIV-2, undifferentiated	2007-05-07 14:23:44	H10	3	\N	3000	\N
31	Y	PRELIMINARY NEGATIVE screen for HIV-1 and HIV-2 antibodies	2007-05-07 14:23:44	H11	3	\N	3100	\N
32	Y	No anti-HIV-1/HIV-2 Plus O antibodies detected.	2007-05-07 14:23:44	H30	3	\N	3200	\N
336	Y	Override result	2007-10-25 10:58:11	\N	36	\N	33600	\N
338	Y	No comments.	2007-11-14 17:03:46.77	\N	3	\N	33800	\N
252	Y	Non-motile	2007-10-08 09:56:08.255	\N	3	\N	25200	\N
253	Y	Test not performed	2007-12-27 03:51:18.518	\N	3	\N	25300	\N
254	Y	Not lysed by phage	2007-10-08 09:56:44.403	\N	3	\N	25400	\N
255	Y	Lysed by phage	2007-10-08 09:57:04.542	\N	3	\N	25500	\N
256	Y	Not performed	2007-10-08 09:57:57.952	\N	3	\N	25600	\N
257	Y	Non-encapsulated	2007-10-08 09:58:19.183	\N	3	\N	25700	\N
258	Y	Encapsulated	2007-10-08 09:58:32.744	\N	3	\N	25800	\N
259	Y	Catalase produced	2007-10-08 09:59:18.243	\N	3	\N	25900	\N
260	Y	No spores	2007-10-08 09:59:32.582	\N	3	\N	26000	\N
261	Y	No Bacillus Anthracis cells detected	2007-10-08 10:02:18.847	\N	3	\N	26100	\N
262	Y	Presumptive result for Bacillus anthracis cells	2007-10-08 10:02:53.064	\N	3	\N	26200	\N
263	Y	No Bacillus anthracis spres detected	2007-10-08 10:03:17.726	\N	3	\N	26300	\N
264	Y	Presumptive results for Bacillus anthracis spores	2007-10-08 10:03:49.025	\N	3	\N	26400	\N
279	Y	Negative: Bacilli in chains	2007-10-08 07:45:50.345	\N	3	\N	27900	\N
280	Y	Negative: Bacilli in no discernable pattern	2007-10-08 07:46:58.699	\N	3	\N	28000	\N
281	Y	Negative: Bacilli in clusters	2007-10-08 07:47:16.713	\N	3	\N	28100	\N
282	Y	Negative: Bacilli in pairs	2007-10-08 07:47:35.311	\N	3	\N	28200	\N
283	Y	Negative: Cocci in chains	2007-10-08 08:02:26.586	\N	3	\N	28300	\N
284	Y	Negative: Cocci in no discernable pattern	2007-10-08 07:48:09.441	\N	3	\N	28400	\N
285	Y	Negative: Cocci in clusters	2007-10-08 07:48:28.11	\N	3	\N	28500	\N
286	Y	Negative: Cocci in pairs	2007-10-08 07:48:43.603	\N	3	\N	28600	\N
287	Y	Negative: Coccobacilli in chains	2007-10-08 07:49:05.522	\N	3	\N	28700	\N
288	Y	Negative: Coccobacilli in no discernable pattern	2007-10-08 07:49:46.483	\N	3	\N	28800	\N
289	Y	Negative: Coccobacilli in clusters	2007-10-08 07:50:05.555	\N	3	\N	28900	\N
290	Y	Negative: Coccobacilli in pairs	2007-10-08 07:50:28.128	\N	3	\N	29000	\N
291	Y	Negative: Other	2007-10-08 07:50:46.049	\N	3	\N	29100	\N
292	Y	Positive: Bacilli in chains	2007-10-08 07:51:33.771	\N	3	\N	29200	\N
293	Y	Positive: Bacilli in no discernable pattern	2007-10-19 14:22:55.748	+B	3	\N	29300	\N
294	Y	Positive: Bacilli in clusters	2007-10-08 07:52:14.538	\N	3	\N	29400	\N
295	Y	Positive: Bacilli in pairs	2007-10-08 07:52:28.789	\N	3	\N	29500	\N
296	Y	Positive: Cocci in chains	2007-10-08 07:52:46.549	\N	3	\N	29600	\N
297	Y	Positive: Cocci in no discernable pattern	2007-10-08 07:53:04.245	\N	3	\N	29700	\N
298	Y	Positive: Cocci in clusters	2007-10-08 07:53:21.216	\N	3	\N	29800	\N
299	Y	Positive: Cocci in pairs	2007-10-08 07:53:41.896	\N	3	\N	29900	\N
300	Y	Positive: Coccobacilli in chains	2007-10-08 07:53:59.557	\N	3	\N	30000	\N
301	Y	Positive: Coccobacilli in no discernable pattern	2007-10-08 07:54:17.456	\N	3	\N	30100	\N
302	Y	Positive: Coccobacilli in clusters	2007-10-08 07:54:42.972	\N	3	\N	30200	\N
303	Y	Positive:  Coccobacilli in pairs	2007-10-08 07:55:06.109	\N	3	\N	30300	\N
304	Y	Positive: Other	2007-10-19 14:20:46.6	+O	3	\N	30400	\N
305	Y	Variable: Bacilli in chains	2007-10-08 07:55:41.877	\N	3	\N	30500	\N
314	Y	Variable: Coccobacilli in no discernable pattern	2007-10-08 07:59:43.354	\N	3	\N	31400	\N
315	Y	Variable: Coccobacilli in clusters	2007-10-08 08:00:03.637	\N	3	\N	31500	\N
316	Y	Variable: Coccobacilli in pairs	2007-10-08 08:00:25.013	\N	3	\N	31600	\N
317	Y	Variable: Other	2007-10-08 08:00:42.907	\N	3	\N	31700	\N
318	Y	Positive: Coccobacilli in pairs	2007-10-08 08:03:43.41	\N	3	\N	31800	\N
319	Y	Roche Fast Start DNA Master Hybridization Probe Kit	2007-10-09 08:36:41.538	\N	3	\N	31900	\N
320	Y	Unsatisfactory	2007-10-09 08:43:16.297	UNS	3	\N	32000	\N
321	Y	Fluorescence detected +1	2007-10-09 08:59:17.731	\N	3	\N	32100	\N
322	Y	Fluorescence detected +2	2007-10-09 08:59:36.507	\N	3	\N	32200	\N
323	Y	Fluorescence detected +3	2007-10-09 08:59:46.012	\N	3	\N	32300	\N
324	Y	Fluorescence detected +4	2007-10-09 08:59:54.726	\N	3	\N	32400	\N
325	Y	No fluorescence detected	2007-10-09 09:00:19.689	\N	3	\N	32500	\N
326	Y	Influenza Virus A RNA Not Detected	2007-10-09 13:43:45.111	\N	3	\N	32600	\N
327	Y	Influenza Virus RNA Detected	2007-10-09 13:48:00.927	\N	3	\N	32700	\N
330	Y	Influenza Virus A/H3 RNA Detected	2007-10-11 08:47:57.952	\N	35	\N	33000	\N
329	Y	Influenza Virus A/H1 RNA Detected	2007-10-11 08:47:33.595	\N	35	\N	32900	\N
331	Y	Influenza Virus A/H5 RNA Detected	2007-10-11 08:50:08.869	\N	35	\N	33100	\N
332	Y	Influenza Virus A RNA Detected	2007-10-11 08:50:34.933	\N	35	\N	33200	\N
333	Y	Influenza Virus A/Untypeable	2007-10-11 08:51:02.16	\N	3	\N	33300	\N
357	Y	Suspect Colonies Present	2007-12-10 08:52:40.007	AAA	3	\N	35700	\N
361	Y	No Suspect Colonies Present	2007-12-11 04:45:11.6	\N	3	\N	36100	\N
362	Y	Gamma Hemolytic Not Detected	2007-12-11 04:46:25.383	\N	3	\N	36200	\N
363	Y	Alpha Hemolytic	2007-12-11 04:46:41.619	\N	3	\N	36300	\N
364	Y	Alpha Hemolytic Not Detected	2007-12-11 04:47:08.684	\N	3	\N	36400	\N
365	Y	Beta Hemolytic	2007-12-11 04:47:17.573	\N	3	\N	36500	\N
366	Y	Beta Hemolytic Not Detected	2007-12-11 04:47:35.056	\N	3	\N	36600	\N
367	Y	Choose Equipment	2007-12-11 05:34:07.166	\N	3	\N	36700	\N
380	Y	Results usually indicate syphilis. A follow-up specimen is recommended to confirm this result unless another specimen has already been submitted within the past 3 months.	2007-12-17 07:36:12.899	RR1	34	\N	38000	\N
381	Y	Results usually indicate syphilis.	2007-12-17 07:36:43.609	RR2	34	\N	38100	\N
382	Y	Nonreactive FTA-ABS test result does not confirm syphilis.	2007-12-17 07:37:16.386	RN	34	\N	38200	\N
383	Y	Results indicate previous syphilis (treated or untreated) or late syphilis. A follow-up specimen is recommended to confirm this result unless another specimen has already been submitted.	2007-12-17 07:38:33.035	NR1	34	\N	38300	\N
14	Y	Giardia lamblia trophozoites	2007-04-02 15:15:41.835	\N	3	\N	1400	\N
15	Y	Entamoeba histolytica cysts	2007-04-02 15:16:08.952	\N	3	\N	1500	\N
229	Y	Viral Agent Not Isolated	2007-10-09 13:25:15.857	VANI	34	\N	22900	\N
384	Y	Results indicate previous syphilis (treated or untreated) or late syphilis.	2007-12-17 07:39:11.179	NR2	34	\N	38400	\N
397	Y	1+	2007-12-27 02:36:49.169	\N	3	\N	39700	\N
406	Y	Dilution 1:10	2007-12-27 03:06:21.748	\N	3	\N	40600	\N
407	Y	Dilution 1:100	2007-12-27 03:06:32.554	\N	3	\N	40700	\N
408	Y	Dilution 1:1000	2007-12-27 03:06:48.844	\N	3	\N	40800	\N
409	Y	No Bacillus anthracis cells detected by TRF	2007-12-27 03:08:09.884	\N	3	\N	40900	\N
410	Y	Presumptive result for Bacillus anthracis cells by TRF	2007-12-27 03:08:48.806	\N	3	\N	41000	\N
411	Y	No Bacillus anthracis sprores detected by TRF	2007-12-27 03:10:11.36	\N	3	\N	41100	\N
412	Y	Presumptive results for Bacillus anthracis spores by TRF	2007-12-27 03:10:54.525	\N	3	\N	41200	\N
413	Y	Susceptible	2007-12-27 03:42:09.733	\N	3	\N	41300	\N
414	Y	Nonsusceptible - Contact CDC for confirmation of resistance	2007-12-27 03:45:53.502	\N	3	\N	41400	\N
415	Y	No Pass	2007-12-27 03:46:15.051	\N	3	\N	41500	\N
416	Y	Pass	2007-12-27 03:46:24.654	\N	3	\N	41600	\N
6	Y	PRELIMINARY POSITIVE	2007-02-13 14:47:16.565	\N	3	\N	600	\N
7	Y	PRELIMINARY NEGATIVE	2007-02-13 14:47:27.368	\N	3	\N	700	\N
8	N	gram + rod	2007-12-11 05:24:10.354	\N	3	\N	800	\N
9	N	gram - rod	2007-12-11 05:24:18.07	\N	3	\N	900	\N
10	Y	Bacillus species (not anthracis)	2007-02-14 14:42:03.911	\N	3	\N	1000	\N
12	Y	Brucella species	2007-03-30 15:22:28.09	\N	3	\N	1200	\N
385	Y	FTA-ABS test result equivocal. A follow-up specimen is recommended in 2-3 weeks to check for a possible early infection.	2007-12-17 07:40:11.015	BOR1	34	\N	38500	\N
386	Y	FTA-ABS test result equivocal.	2007-12-17 07:40:31.918	BOR2	34	\N	38600	\N
387	Y	Antibody detected may be maternal. Other criteria must be used to rule out congenital syphilis.	2007-12-18 05:29:09.034	INF	34	\N	38700	\N
388	Y	Staining is atypical. Results are uninterpretable. Please submit another specimen.	2007-12-17 07:41:59.546	ATYP	34	\N	38800	\N
398	Y	1/2+	2007-12-27 02:37:25.718	\N	3	\N	39800	\N
399	Y	2+	2007-12-27 02:37:34.275	\N	3	\N	39900	\N
400	Y	2/3+	2007-12-27 02:37:44.413	\N	3	\N	40000	\N
401	Y	3+	2007-12-27 02:37:52.447	\N	3	\N	40100	\N
402	Y	3/4+	2007-12-27 02:38:00.824	\N	3	\N	40200	\N
403	Y	4+	2007-12-27 02:38:09.595	\N	3	\N	40300	\N
404	Y	FTA Reactive	2007-12-27 02:39:48.232	F	34	\N	40400	\N
405	Y	Fluorescence	2007-12-27 02:40:02.324	R	34	\N	40500	\N
417	Y	< 0.002 ug/ml	2007-12-27 07:31:23.462	\N	3	\N	41700	\N
418	Y	= 0.002 ug/ml	2007-12-27 07:31:40.039	\N	3	\N	41800	\N
419	Y	=0.003 ug/ml	2007-12-27 07:31:59.163	\N	3	\N	41900	\N
420	Y	=0.004 ug/ml	2007-12-27 07:32:12.161	\N	3	\N	42000	\N
421	Y	=0.006 ug/ml	2007-12-27 07:32:25.987	\N	3	\N	42100	\N
422	Y	=0.008 ug/ml	2007-12-27 07:32:40.565	\N	3	\N	42200	\N
423	Y	=0.012 ug/ml	2007-12-27 07:32:54.853	\N	3	\N	42300	\N
424	Y	=0.016 ug/ml	2007-12-27 07:33:07.974	\N	3	\N	42400	\N
425	Y	=0.023 ug/ml	2007-12-27 07:33:19.043	\N	3	\N	42500	\N
426	Y	=0.032 ug/ml	2007-12-27 07:33:33.124	\N	3	\N	42600	\N
427	Y	=0.047 ug/ml	2007-12-27 07:33:45.267	\N	3	\N	42700	\N
428	Y	=0.064 ug/ml	2007-12-27 07:33:58.572	\N	3	\N	42800	\N
429	Y	=0.094 ug/ml	2007-12-27 07:34:11.959	\N	3	\N	42900	\N
430	Y	=0.125 ug/ml	2007-12-27 07:34:28.266	\N	3	\N	43000	\N
431	Y	=0.19 ug/ml	2007-12-27 07:34:45.808	\N	3	\N	43100	\N
432	Y	=0.25 ug/ml	2007-12-27 07:34:56.729	\N	3	\N	43200	\N
433	Y	=0.38 ug/ml	2007-12-27 07:35:09.267	\N	3	\N	43300	\N
434	Y	=0.5 ug/ml	2007-12-27 07:35:21.777	\N	3	\N	43400	\N
435	Y	=0.75 ug/ml	2007-12-27 07:35:33.775	\N	3	\N	43500	\N
436	Y	=1.0 ug/ml	2007-12-27 07:35:45.211	\N	3	\N	43600	\N
437	Y	=1.5 ug/ml	2007-12-27 07:35:56.407	\N	3	\N	43700	\N
438	N	=2.3 ug/ml	2007-12-27 08:29:59.506	\N	3	\N	43800	\N
439	N	=4.6 ug/ml	2007-12-27 08:33:16.223	\N	3	\N	43900	\N
440	Y	=8 ug/ml	2007-12-27 07:36:35.588	\N	3	\N	44000	\N
441	Y	=12 ug/ml	2007-12-27 07:36:46.709	\N	3	\N	44100	\N
442	Y	=16 ug/ml	2007-12-27 07:36:55.681	\N	3	\N	44200	\N
443	Y	=24 ug/ml	2007-12-27 07:37:23.01	\N	3	\N	44300	\N
444	Y	=32 ug/ml	2007-12-27 07:37:38.262	\N	3	\N	44400	\N
445	Y	> 32 ug/ml	2007-12-27 07:37:55.913	\N	3	\N	44500	\N
446	Y	< 0.016 ug/ml	2007-12-27 07:38:44.343	\N	3	\N	44600	\N
447	Y	=48 ug/ml	2007-12-27 07:39:15.52	\N	3	\N	44700	\N
448	Y	=64 ug/ml	2007-12-27 07:39:25.217	\N	3	\N	44800	\N
449	Y	=96 ug/ml	2007-12-27 07:39:36.704	\N	3	\N	44900	\N
450	Y	=128 ug/ml	2007-12-27 07:39:52.246	\N	3	\N	45000	\N
451	Y	=192 ug/ml	2007-12-27 07:40:02.528	\N	3	\N	45100	\N
452	Y	=256 ug/ml	2007-12-27 07:40:16.527	\N	3	\N	45200	\N
453	Y	> 256 ug/ml	2007-12-27 07:40:25.681	\N	3	\N	45300	\N
454	Y	Tetracycline	2007-12-27 07:42:15.347	\N	3	\N	45400	\N
455	Y	=2.0 ug/ml	2007-12-27 08:30:11.726	\N	3	\N	45500	\N
456	Y	=3.0 ug/ml	2007-12-27 08:30:19.976	\N	3	\N	45600	\N
457	Y	=4.0 ug/ml	2007-12-27 08:33:31.812	\N	3	\N	45700	\N
458	Y	=6.0 ug/ml	2007-12-27 08:33:42.375	\N	3	\N	45800	\N
477	Y	Influenza Virus A/H7 RNA Detected	2008-01-23 08:05:11.335	\N	35	\N	47700	\N
478	Y	Influenza Virus A/H9 RNA Detected	2008-01-23 08:05:36.034	\N	35	\N	47800	\N
45	Y	Influenza Virus A/H1	2007-10-11 08:36:26.062	\N	35	\N	4500	\N
46	Y	Influenza Virus A/H7	2007-10-11 08:36:50.383	\N	35	\N	4600	\N
47	Y	Influenza Virus A/H9	2007-10-11 08:36:58.426	\N	35	\N	4700	\N
337	Y	dummy placeholder	2007-11-14 14:06:05.175	\N	3	\N	33700	\N
49	Y	INDETERMINATE	2006-09-28 09:02:04.359	\N	34	\N	4900	\N
50	Y	UNSATISFACTORY	2006-09-20 16:28:00	\N	35	\N	5000	\N
11	Y	Coded comments need to be added	2007-02-27 11:11:45.697	\N	3	\N	1100	\N
52	Y	NOT DETECTED	2006-10-05 12:29:29.094	\N	3	\N	5200	\N
13	Y	Giardia lamblia cysts	2007-04-02 15:15:17.813	\N	3	\N	1300	\N
54	Y	REACTIVE	2006-10-31 11:40:34.312	\N	34	\N	5400	\N
55	Y	NONREACTIVE	2006-09-07 08:47:17	\N	34	\N	5500	\N
147	Y	POSITIVE = Any two of the following bands present: p24,gp41, and gp 120/160. Other viral bands may be present. Indicates patient is positive for anti-HIV-1 antibody.	2007-02-13 13:49:27.544	\N	34	\N	14700	\N
33	Y	FINAL INTERPRETATION: These results most closely indicate the presence of HIV-1 antibodies.	2007-05-07 14:27:13	H21	3	\N	3300	\N
58	Y	MID = Patient ID ON Specimen and Request Mismatched	2007-10-09 13:23:44.957	\N	34	\N	5800	\N
59	Y	NR = Specimen/Culture Not Received	2007-10-09 13:24:37.161	\N	34	\N	5900	\N
60	Y	GH = Gross Hemolysis of Specimen	2007-10-09 13:22:48.171	\N	34	\N	6000	\N
61	Y	ANO = Please Submit Another Specimen.	2007-10-09 13:22:23.13	\N	34	\N	6100	\N
148	Y	NEGATIVE = No bands are present indicating absence of anti-HIV-1 antibody. Please submit another OraSure (or serum) sample one month following the first sample for repeat testing.	2007-02-13 14:03:58.845	\N	34	\N	14800	\N
67	Y	Influenza Virus A/H5	2007-10-11 08:36:42.02	\N	35	\N	6700	\N
115	Y	SENSITIVE	2006-10-03 09:25:20	\N	3	\N	11500	\N
120	Y	Preliminary	2007-10-09 13:31:55.42	\N	3	\N	12000	\N
121	Y	FINAL	2006-10-03 09:48:58	\N	3	\N	12100	\N
127	Y	Bacillus anthracis not found	2006-10-11 14:25:42	\N	3	\N	12700	\N
128	Y	+	2006-10-23 14:46:29.656	\N	3	\N	12800	\N
358	Y	Variable: Bacilli in pairs	2007-12-11 03:18:22.068	\N	3	\N	35800	\N
44	Y	Influenza Virus A/H3	2007-10-11 08:36:34.112	\N	35	\N	4400	\N
66	Y	INDETERMINATE = Band pattern does not meet criteria for positive. Repeatedly similar indeterminate patterns indicate bands not specific for HIV. Please submit another OraSure (or serum) sample one month following the first sample for repeat testing.	2007-02-13 13:52:38.554	\N	34	\N	6600	\N
77	Y	No Growth	2007-10-09 13:29:29.012	\N	3	\N	7700	\N
359	Y	Spores Present	2007-12-11 03:49:49.524	\N	3	\N	35900	\N
360	Y	Atypical Suspect Colonies	2007-12-11 04:16:34.021	\N	3	\N	36000	\N
368	Y	ABI 7500	2007-12-11 05:37:39.257	\N	3	\N	36800	\N
34	Y	FINAL INTERPRETATION: These results most closely indicate the presence of HIV-2 antibodies; however, confirmation testing for HIV-2 will be performed at the Centers for Disease Control and Prevention on this current specimen.	2007-05-07 14:27:13	H22	3	\N	3400	\N
80	Y	Gamma Hemolytic	2006-09-20 16:35:46	\N	3	\N	8000	\N
378	Y	BORDERLINE	2007-12-17 07:34:03.607	BOR	3	\N	37800	\N
35	Y	FINAL INTERPRETATION: These results most closely indicate the presence of HIV antibodies (without differentiation). Please submit another specimen one month following the collection of the first specimen for repeat testing. This subsequent specimen may allow for HIV antibody differentiation.	2007-05-07 14:27:13	H23	3	\N	3500	\N
36	Y	FINAL INTERPRETATION: These results most closely indicate the presence of HIV-1 antibodies. Please submit another specimen one month following the first specimen for repeat testing. This subsequent specimen may allow for confirmation of HIV-1 antibodies.	2007-05-07 14:27:13	H24	3	\N	3600	\N
497	Y	A	2009-07-31 10:02:01.984		3	\N	49700	\N
498	Y	AB	2009-07-31 10:04:27.937		34	\N	49800	\N
499	Y	O	2009-07-31 10:04:53.609		35	\N	49900	\N
501	Y	A	\N	\N	\N	\N	50100	\N
377	Y	Réactif	2007-12-17 07:33:39.319	R	3	\N	37700	\N
660	Y	Demographic Response Yes (in Yes, No or Unknown)	\N	Yes	57	answer.yes	66000	\N
661	Y	Demographic Response No (Yes, No or Unknown)	\N	No	57	answer.no	66100	\N
662	Y	Demographic Response Unknown (Yes, No or Unknown)	\N	Unknown	57	answer.unknown	66200	\N
720	Y	The data entry has not yet started	2010-10-28 06:10:34.12594	Not Start	77	record.notStarted	72000	\N
721	Y	The initial entry has been done	2010-10-28 06:10:34.12594	Init Ent	77	record.initialEntry	72100	\N
722	Y	The second, validation, entry has been done	2010-10-28 06:10:34.12594	Valid Ent	77	record.validationEntry	72200	\N
723	Y	There is reason to suspect that the form was not filled out correctly	2010-10-28 06:10:34.12594	Under Inv	77	record.underInvestigation	72300	\N
379	Y	Non Réactif	2007-12-17 07:34:19.397	NR	3	\N	37900	\N
63	Y	Négatif	2006-09-20 16:28:31	\N	3	\N	6300	\N
62	Y	Positif	2006-09-20 16:28:29	\N	3	\N	6200	\N
94	Y	Indéterminé	2006-09-20 16:32:51	\N	3	\N	9400	\N
820	Y	Positive	2011-02-16 12:45:59.612333	\N	117	dict.HIVResult.positive	82000	\N
821	Y	Negative	2011-02-16 12:45:59.612333	\N	117	dict.HIVResult.negative	82100	\N
822	Y	Indeterminate	2011-02-16 12:45:59.612333	\N	117	dict.HIVResult.indeterminate	82200	\N
840	Y	Refrigerated	2011-02-16 14:48:42.955979	Refrig	137	dictionary.condition.Regrigerated	84000	\N
841	Y	Not refrigerated	2011-02-16 14:48:42.955979	notRefrig	137	dictionary.condition.notRegrigerated	84100	\N
842	Y	Frozen	2011-02-16 14:48:42.955979	Frozen	137	dictionary.condition.frozen	84200	\N
843	Y	Leaked Sample	2011-02-16 14:48:42.955979	leaked	137	dictionary.condition.leakedTube	84300	\N
844	Y	Broken Tubes	2011-02-16 14:48:42.955979	broken	137	dictionary.condition.brokenTube	84400	\N
860	Y	Ouest	2011-03-29 15:37:16.61796	\N	157	\N	86000	\N
861	Y	Nord	2011-03-29 15:37:16.61796	\N	157	\N	86100	\N
862	Y	Sud	2011-03-29 15:37:16.61796	\N	157	\N	86200	\N
863	Y	Artibonite	2011-03-29 15:37:16.61796	\N	157	\N	86300	\N
864	Y	Sud-Est	2011-03-29 15:37:16.61796	\N	157	\N	86400	\N
865	Y	Nord'Ouest	2011-03-29 15:37:16.61796	\N	157	\N	86500	\N
866	Y	Centre	2011-03-29 15:37:16.61796	\N	157	\N	86600	\N
867	Y	Grand'Anse	2011-03-29 15:37:16.61796	\N	157	\N	86700	\N
868	Y	Nord'Est	2011-03-29 15:37:16.61796	\N	157	\N	86800	\N
869	Y	Nippes	2011-03-29 15:37:16.61796	\N	157	\N	86900	\N
870	Y	Hemolysis	2011-03-29 15:37:16.698341	Hemo	137	 dictionary.condition.hematolysis	87000	\N
871	Y	Coagulated	2011-03-29 15:37:16.698341	Coag	137	dictionary.condition.coagulated	87100	\N
872	Y	Insuffient Amount	2011-03-29 15:37:16.698341	Insuff	137	dictionary.condition.insufficient	87200	\N
873	Y	Contaminated	2011-03-29 15:37:16.698341	Contamin	137	dictionary.condition.contaminated	87300	\N
874	Y	Inadequate Sampling	2011-03-29 15:37:16.698341	Inadequ	137	dictionary.condition.inadequate	87400	\N
875	Y	Overturned Sample	2011-03-29 15:37:16.698341	Overturned	137	dictionary.condition.overturned	87500	\N
876	Y	Other	2011-03-29 15:37:16.698341	Other	137	dictionary.condition.other	87600	\N
877	Y	Sample without Form	2011-03-29 15:37:16.698341	NoForm	137	dictionary.condition.sampleNoForm	87700	\N
878	Y	Form without Sample	2011-03-29 15:37:16.698341	NoSample	137	dictionary.condition.formNoSample	87800	\N
880	Y	Couleur: Marron	2011-04-11 11:42:45.485352	\N	3	 dict.color.brown 	88000	\N
881	Y	Couleur: Jaunâtre	2011-04-11 11:42:45.485352	\N	3	 dict.color.yellowish 	88100	\N
882	Y	Couleur: Verdatre	2011-04-11 11:42:45.485352	\N	3	 dict.color.greenish 	88200	\N
883	Y	Couleur: Blanchâtre	2011-04-11 11:42:45.485352	\N	3	 dict.color.whitish 	88300	\N
884	Y	Consistance: molle	2011-04-11 11:42:45.485352	\N	3	 dict.consistency.soft 	88400	\N
885	Y	Consistance: glaireuse	2011-04-11 11:42:45.485352	\N	3	 dict.consistency.slimy 	88500	\N
886	Y	Consistance: pâteuse	2011-04-11 11:42:45.485352	\N	3	 dict.consistency.pasty  	88600	\N
887	Y	Consistance: ferme	2011-04-11 11:42:45.485352	\N	3	 dict.consistency.firm 	88700	\N
888	Y	Consistance: liquide	2011-04-11 11:42:45.485352	\N	3	 dict.consistency.liquid 	88800	\N
889	Y	Consistance: semi-liquide	2011-04-11 11:42:45.485352	\N	3	 dict.consistency.semiliquid 	88900	\N
900	Y	Rare	2011-04-22 10:58:27.26805	\N	\N	\N	90000	\N
980	Y	Oxalate de Ca	2011-05-04 08:36:50.930563	\N	\N	\N	98000	\N
981	Y	Phosphate Amorphes	2011-05-04 08:36:50.930563	\N	\N	\N	98100	\N
982	Y	Phosphate Triples	2011-05-04 08:36:50.930563	\N	\N	\N	98200	\N
983	Y	Phosphate de Ca	2011-05-04 08:36:50.930563	\N	\N	\N	98300	\N
984	Y	Phosphate de Mg	2011-05-04 08:36:50.930563	\N	\N	\N	98400	\N
985	Y	Sulfate de Ca	2011-05-04 08:36:50.930563	\N	\N	\N	98500	\N
986	Y	Urates Amorphes	2011-05-04 08:36:50.930563	\N	\N	\N	98600	\N
987	Y	Urates d'Ammonium	2011-05-04 08:36:50.930563	\N	\N	\N	98700	\N
988	Y	Acid Urique	2011-05-04 08:36:50.930563	\N	\N	\N	98800	\N
1000	Y	Invalide	2011-05-05 13:58:38.564045	\N	\N	\N	100000	\N
1040	Y	fr-FR	2012-05-14 11:24:08.802607	fr-FR	197	\N	104000	\N
1041	Y	en-US	2012-05-14 11:24:08.802607	en-US	197	\N	104100	\N
1080	Y	none	2012-06-11 09:33:47.062073	\N	217	dictionary.education.none	108000	\N
1081	Y	primary	2012-06-11 09:33:47.062073	\N	217	dictionary.education.primary	108100	\N
1082	Y	secondary	2012-06-11 09:33:47.062073	\N	217	dictionary.education.secondary	108200	\N
1083	Y	upper	2012-06-11 09:33:47.062073	\N	217	dictionary.education.upper	108300	\N
1084	Y	single	2012-06-11 09:33:47.062073	\N	218	dictionary.marriage.single	108400	\N
1085	Y	married	2012-06-11 09:33:47.062073	\N	218	dictionary.marriage.married	108500	\N
1086	Y	livingWith	2012-06-11 09:33:47.062073	\N	218	dictionary.marriage.livingWith	108600	\N
1087	Y	divorced	2012-06-11 09:33:47.062073	\N	218	dictionary.marriage.divorced	108700	\N
1088	Y	widowed	2012-06-11 09:33:47.062073	\N	218	dictionary.marriage.widowed	108800	\N
1089	Y	DNA	2012-06-11 09:33:47.062073	\N	218	dictionary.marriage.DNA	108900	\N
1090	Y	Ivoirian	2012-06-11 09:33:47.062073	\N	219	dictionary.nationality.Ivoirian	109000	\N
1091	Y	West African	2012-06-11 09:33:47.062073	\N	219	dictionary.nationality.african.west	109100	\N
1092	Y	African	2012-06-11 09:33:47.062073	\N	219	dictionary.nationality.african	109200	\N
1093	Y	other	2012-06-11 09:33:47.062073	\N	219	dictionary.nationality.other	109300	\N
1020	N	paidInFull	2012-03-13 15:32:16.552198	\N	177	dictionary.patient.payment.full	102000	\N
1021	N	paidPartial	2012-03-13 15:32:16.552198	\N	177	dictionary.patient.payment.partial	102100	\N
1022	N	paidNone	2012-03-13 15:32:16.552198	\N	177	dictionary.patient.payment.none	102200	\N
1120	Y	normalCash	2013-08-05 11:35:36.498381	\N	177	dictionary.patient.payment.normal.cash	112000	\N
1121	Y	reducedCash	2013-08-05 11:35:36.498381	\N	177	dictionary.patient.payment.reduced.cash	112100	\N
1122	Y	normalInsurance	2013-08-05 11:35:36.498381	\N	177	dictionary.patient.payment.normal.insurance	112200	\N
1123	Y	reducedInsurance	2013-08-05 11:35:36.498381	\N	177	dictionary.patient.payment.reduced.insurance	112300	\N
1140	Y	Measurement not available due to technical problem. Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.technical.problem	114000	\N
1141	Y	 Sample / request form not / misidentified . Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.form.misidentified	114100	\N
1142	Y	Sample / request form incompatible . Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.form.incompatible	114200	\N
1143	Y	Free sample request form or vice versa. Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.free.request.form	114300	\N
1144	Y	Sample transported / stored incorrectly. Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.stored.incorrectly	114400	\N
1145	Y	Identification of improper sample. Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.identification.improper	114500	\N
1146	Y	Sample was collected in the wrong tube. Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.wrong.tube	114600	\N
553	Y	Dracunculus Medinensis	2009-07-31 10:50:41.812	DRAC MED	38	dictionary.result.Dracunculus_Medinensis	55300	\N
1147	Y	 Incorrect quantity of the sample. Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.incorrect.quantity	114700	\N
1148	Y	The sample received is not suitable for test requested . Please submit the appropriate sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.not.suitable	114800	\N
1149	Y	The sample received is coagulated . Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.coagulated	114900	\N
1150	Y	The received sample was refrigerated . Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.was.referigerated	115000	\N
1151	Y	Tube received arrived broken or spilled . Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.broken.tube	115100	\N
1152	Y	The received sample is hemolyzed . Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.hemolyzed	115200	\N
1153	Y	The sample is contaminated. Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.contaminated	115300	\N
1154	Y	The sample received was insufficient volume . Please submit another sample.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.insufficient.volume	115400	\N
1155	Y	Following a mistake in sample intake, a not requested test has been scheduled but not executed.	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.mistake.not.executed	115500	\N
1156	Y	Please submit another sample. Need to re-test .	2014-05-01 20:40:55.867239	\N	257	dictionary.reject.result.re-test	115600	\N
1160	Y	The sample received is jaundiced. Please submit another sample.	2020-01-22 21:46:10.883714	\N	257	dictionary.reject.result.jaundiced	115233	\N
1161	Y	The sample received is lipemic. Please submit another sample.	2020-01-22 21:46:10.883714	\N	257	dictionary.reject.result.lipemic	115266	\N
1162	Y	Other.	2020-01-22 21:46:10.883714	\N	257	dictionary.reject.result.other	116200	\N
1163	Y	Routine Testing	2020-01-22 21:46:11.052657	Routine	277	dictionary.program.routine	10	\N
1164	Y	HIV Program - Initial Visit	2020-01-22 21:46:11.052657	Initial Visit	277	dictionary.program.initial	20	\N
1165	Y	HIV Program Follow up	2020-01-22 21:46:11.052657	Follow up	277	dictionary.program.followUp	30	\N
641	Y	P51	2010-01-12 13:53:16.312	P51	38	dictionary.result.P51	64100	\N
663	Y	Filament Mycelium	2010-06-11 13:00:20.364	MYCELIUM	38	dictionary.result.Filament_Mycelium	66300	\N
680	Y	Levures bourgeonnante	2010-06-11 13:01:37.136	BOURGEON	38	dictionary.result.Levures_bourgeonnante	68000	\N
502	Y	Trace	2009-07-31 10:33:15.796	TRACE	38	dictionary.result.Trace	50200	\N
504	Y	AB	2009-07-31 10:33:37.953	AB	38	dictionary.result.AB	50400	\N
506	Y	Wucheria Bancrofti	2009-07-31 10:34:20.468	WUCH BANC	38	dictionary.result.Wucheria_Bancrofti	50600	\N
507	Y	Manzonella Ozardi	2009-07-31 10:34:38.765	MANZ OZ	38	dictionary.result.Manzonella_Ozardi	50700	\N
508	Y	Forme trophozoite	2009-07-31 10:34:55.468	F TROPH	38	dictionary.result.Forme_trophozoite	50800	\N
509	Y	Forme Gametocyte	2009-07-31 10:35:18.484	F GAM	38	dictionary.result.Forme_Gametocyte	50900	\N
510	Y	Forme Shizogonique	2009-07-31 10:35:38.015	F SHIZ	38	dictionary.result.Forme_Shizogonique	51000	\N
511	Y	Intermediare	2009-07-31 10:35:58.671	INTER	38	dictionary.result.Intermediare	51100	\N
516	Y	En Amas	2009-07-31 10:37:49.937	AMAS	38	dictionary.result.En_Amas	51600	\N
517	Y	Liquide	2009-07-31 10:38:10.468	LIQU	38	dictionary.result.Liquide	51700	\N
518	Y	Diarrheique	2009-07-31 10:38:32	DIAR	38	dictionary.result.Diarrheique	51800	\N
519	Y	Molle	2009-07-31 10:38:47.187	MOLL	38	dictionary.result.Molle	51900	\N
521	Y	Moulée	2009-07-31 10:39:30.14	MOUL	38	dictionary.result.Moulee	52100	\N
522	Y	Dure	2009-07-31 10:39:46.218	DURE	38	dictionary.result.Dure	52200	\N
523	Y	Isospora Belli	2009-07-31 10:40:09.546	ISO BELLI	38	dictionary.result.Isospora_Belli	52300	\N
524	Y	Cryptosporidium Parvum	2009-07-31 10:40:24.125	CRYP PARV	38	dictionary.result.Cryptosporidium_Parvum	52400	\N
525	Y	Cryptosporidium Cayetanensis	2009-07-31 10:40:42.046	CRYP CAY	38	dictionary.result.Cryptosporidium_Cayetanensis	52500	\N
526	Y	Cyclospora	2009-07-31 10:41:01.093	CYCLOSPORA	38	dictionary.result.Cyclospora	52600	\N
527	Y	Cryptosporidium canum	2009-07-31 10:41:24.296	CRYP CANUM	38	dictionary.result.Cryptosporidium_canum	52700	\N
528	Y	Toxoplasma Gondii	2009-07-31 10:41:39.625	TOX GONDII	38	dictionary.result.Toxoplasma_Gondii	52800	\N
529	Y	Cryptosporidium andersoni	2009-07-31 10:41:58.812	CRYP ANDE	38	dictionary.result.Cryptosporidium_andersoni	52900	\N
531	Y	Cryptosporidium bailey	2009-07-31 10:42:18.218	CRYP BAIL	38	dictionary.result.Cryptosporidium_bailey	53100	\N
532	Y	Cryptosporidium canis	2009-07-31 10:42:31.984	CRYP CANIS	38	dictionary.result.Cryptosporidium_canis	53200	\N
533	Y	Cryptosporidium felis	2009-07-31 10:42:46.406	CRYP FELIS	38	dictionary.result.Cryptosporidium_felis	53300	\N
534	Y	Cryptosporidium galli	2009-07-31 10:43:19.968	CRYP GALLI	38	dictionary.result.Cryptosporidium_galli	53400	\N
535	Y	Cryptosporidium hominis	2009-07-31 10:43:38.593	CRYP HOMIN	38	dictionary.result.Cryptosporidium_hominis	53500	\N
536	Y	Cryptosporidium meleagridis	2009-07-31 10:43:52.828	CRYP MELEA	38	dictionary.result.Cryptosporidium_meleagridis	53600	\N
537	Y	Cryptosporidium muris	2009-07-31 10:44:03.921	CRYP MURIS	38	dictionary.result.Cryptosporidium_muris	53700	\N
538	Y	Cryptosporidium saurophilum	2009-07-31 10:44:44.187	CRYP SAUR	38	dictionary.result.Cryptosporidium_saurophilum	53800	\N
539	Y	Cryptosporidium serpentis	2009-07-31 10:44:57.812	CRYP SERP	38	dictionary.result.Cryptosporidium_serpentis	53900	\N
540	Y	Cryptosporidium wrairi	2009-07-31 10:45:13.5	CRYP WRAIR	38	dictionary.result.Cryptosporidium_wrairi	54000	\N
541	Y	Tres Positive	2009-07-31 10:45:40.156	TRES POS	38	dictionary.result.Tres_Positive	54100	\N
542	Y	Positif	2009-07-31 10:45:59.906	POSITIF	38	dictionary.result.Positif	54200	\N
543	Y	Faiblement Positif	2009-07-31 10:46:22.171	F POS	38	dictionary.result.Faiblement_Positif	54300	\N
544	Y	Negatif	2009-07-31 10:46:33.984	NEG	38	dictionary.result.Negatif	54400	\N
545	Y	Normal	2009-07-31 10:47:12.468	NORM	38	dictionary.result.Normal	54500	\N
546	Y	Anormal	2009-07-31 10:48:06.64	ANORM	38	dictionary.result.Anormal	54600	\N
547	Y	Ascaris Lumbricoides	2009-07-31 10:48:52.656	ASC LUMB	38	dictionary.result.Ascaris_Lumbricoides	54700	\N
548	Y	Enterobius Vermicularis	2009-07-31 10:49:08.578	ENT VERM	38	dictionary.result.Enterobius_Vermicularis	54800	\N
549	Y	Necator Americanus	2009-07-31 10:49:25.14	NEC AM	38	dictionary.result.Necator_Americanus	54900	\N
550	Y	Ancyclostoma Duodenale	2009-07-31 10:49:44.468	ANC DU	38	dictionary.result.Ancyclostoma_Duodenale	55000	\N
551	Y	Trichnella Spiralis	2009-07-31 10:50:01.765	TRICH SPIR	38	dictionary.result.Trichnella_Spiralis	55100	\N
552	Y	Toxocara Canis	2009-07-31 10:50:21.5	TOX CAN	38	dictionary.result.Toxocara_Canis	55200	\N
554	Y	Entamoeba Histolitica	2009-07-31 10:51:02.718	ENT HIST	38	dictionary.result.Entamoeba_Histolitica	55400	\N
555	Y	Entamoeba Coli	2009-07-31 10:51:14.984	ENT COLI	38	dictionary.result.Entamoeba_Coli	55500	\N
556	Y	Iodamoeba	2009-07-31 10:51:43.328	IODAMOEBA	38	dictionary.result.Iodamoeba	55600	\N
557	Y	Giardia	2009-07-31 10:51:55	GIARDIA	38	dictionary.result.Giardia	55700	\N
558	Y	Lamblia	2009-07-31 10:52:06.078	LAMBLIA	38	dictionary.result.Lamblia	55800	\N
559	Y	Trichomonas Intestinalis	2009-07-31 10:52:32.562	TRICH INT	38	dictionary.result.Trichomonas_Intestinalis	55900	\N
560	Y	Trouble	2009-07-31 10:53:14.656	TROUBLE	38	dictionary.result.Trouble	56000	\N
513	Y	Positif (++)	2009-07-31 10:36:51	++	38	dictionary.result.Positif_(++)	51300	\N
514	Y	Positif (+++)	2009-07-31 10:37:07.5	+++	38	dictionary.result.Positif_(+++)	51400	\N
515	Y	Positif (++++)	2009-07-31 10:37:23.781	++++	38	dictionary.result.Positif_(++++)	51500	\N
561	Y	Clair	2009-07-31 10:53:32.218	CLAIR	38	dictionary.result.Clair	56100	\N
562	Y	lipemique	2009-07-31 10:53:44.484	LIPEMIQUE	38	dictionary.result.lipemique	56200	\N
563	Y	icterique	2009-07-31 10:53:55.859	ICTERIQUE	38	dictionary.result.icterique	56300	\N
564	Y	Autre	2009-07-31 10:54:14.468	AUTRE	38	dictionary.result.Autre	56400	\N
565	Y	Indeterminé	2009-07-31 10:55:02.437	IND	38	dictionary.result.Indetermine	56500	\N
566	Y	Jaune Paille	2009-07-31 10:55:31.296	JAU PAI	38	dictionary.result.Jaune_Paille	56600	\N
567	Y	Jaune Citrin	2009-07-31 10:55:48.484	JAU CIT	38	dictionary.result.Jaune_Citrin	56700	\N
568	Y	Jaune Foncee	2009-07-31 10:56:08.375	JAU FONC	38	dictionary.result.Jaune_Foncee	56800	\N
569	Y	Rouge	2009-07-31 10:56:24.093	ROUG	38	dictionary.result.Rouge	56900	\N
570	Y	Marron	2009-07-31 10:56:38.109	MARR	38	dictionary.result.Marron	57000	\N
571	Y	Fonce	2009-07-31 10:56:55.39	FONCE	38	dictionary.result.Fonce	57100	\N
572	Y	Granuleux	2009-07-31 10:57:35.468	GRAN	38	dictionary.result.Granuleux	57200	\N
573	Y	Cireux	2009-07-31 10:57:52.062	CIREUX	38	dictionary.result.Cireux	57300	\N
505	Y	O+	2009-07-31 10:33:44.359	O	38	dictionary.result.O+	50500	\N
574	Y	Hyalin	2009-07-31 10:58:03.796	HYALIN	38	dictionary.result.Hyalin	57400	\N
512	Y	Positif (+)	2009-07-31 10:36:35.953	+	38	dictionary.result.Positif_(+)	51200	\N
575	Y	Entamoeba Hartmani	\N	ENT HART	38	dictionary.result.Entamoeba_Hartmani	57500	\N
576	Y	Endolimax Nana	\N	END NANA	38	dictionary.result.Endolimax_Nana	57600	\N
577	Y	Dientamoeba Fragilis	\N	DIEN FRAG	38	dictionary.result.Dientamoeba_Fragilis	57700	\N
578	Y	Chilomastix Mesnili	\N	CHIL MES	38	dictionary.result.Chilomastix_Mesnili	57800	\N
579	Y	Balantidium Coli	\N	BAL COLI	38	dictionary.result.Balantidium_Coli	57900	\N
580	Y	Ankylostome Duodenale	2009-08-05 22:08:36.758	ANK DUO	38	dictionary.result.Ankylostome_Duodenale	58000	\N
620	Y	GP120	2010-01-12 13:52:20.146	GP120	38	dictionary.result.GP120	62000	\N
621	Y	P31	2010-01-12 13:53:32.533	P31	38	dictionary.result.P31	62100	\N
602	Y	GP41	2010-01-12 13:53:49.129	GP41	38	dictionary.result.GP41	60200	\N
642	Y	P24	2010-01-12 13:54:05.318	P24	38	dictionary.result.P24	64200	\N
622	Y	P17	2010-01-12 13:54:18.086	P17	38	dictionary.result.P17	62200	\N
665	Y	Leucocytaire	2010-06-11 13:02:26.365	LEUCOCYTA	38	dictionary.result.Leucocytaire	66500	\N
666	Y	Hematique	2010-06-11 13:02:43.815	HEMATIQUE	38	dictionary.result.Hematique	66600	\N
667	Y	Forme Gametocyte +	2010-06-11 13:38:31.725	F GAM +	38	dictionary.result.Forme_Gametocyte_+	66700	\N
668	Y	Forme Gametocyte ++	2010-06-11 13:38:52.16	F GAM ++	38	dictionary.result.Forme_Gametocyte_++	66800	\N
669	Y	Forme Gametocyte +++	2010-06-11 13:39:07.458	F GAM +++	38	dictionary.result.Forme_Gametocyte_+++	66900	\N
670	Y	Forme Trophozoite +	2010-06-11 13:39:28.926	FT +	38	dictionary.result.Forme_Trophozoite_+	67000	\N
671	Y	Forme Trophozoite ++	2010-06-11 13:39:45.658	FT ++	38	dictionary.result.Forme_Trophozoite_++	67100	\N
672	Y	Forme Trophozoite +++	2010-06-11 13:40:00.668	FT +++	38	dictionary.result.Forme_Trophozoite_+++	67200	\N
673	Y	Forme Schizogonique +	2010-06-11 13:40:33.045	F SCH +	38	dictionary.result.Forme_Schizogonique_+	67300	\N
674	Y	Forme Schizogonique ++	2010-06-11 13:41:00.281	F SCH ++	38	dictionary.result.Forme_Schizogonique_++	67400	\N
675	Y	Forme Schizogonique +++	2010-06-11 13:41:14.423	F SCH +++	38	dictionary.result.Forme_Schizogonique_+++	67500	\N
681	Y	Verdatre	2010-06-11 13:47:22.95	VERDATRE	38	dictionary.result.Verdatre	68100	\N
682	Y	filament mycelium +	2010-06-11 13:51:11.334	FIL MYC +	38	dictionary.result.filament_mycelium_+	68200	\N
683	Y	Filament Mycelium ++	2010-06-11 13:51:26.629	FIL MYC ++	38	dictionary.result.Filament_Mycelium_++	68300	\N
687	Y	Filament Mycelium +++	2010-06-11 13:52:02.1	FIL MYC+++	38	dictionary.result.Filament_Mycelium_+++	68700	\N
688	Y	Levures Bourgeonnante +	2010-06-11 13:52:50.848	BOURG +	38	dictionary.result.Levures_Bourgeonnante_+	68800	\N
689	Y	Levures Bourgennante ++	2010-06-11 13:53:17.484	BOURG ++	38	dictionary.result.Levures_Bourgennante_++	68900	\N
690	Y	Levures Bourgeonnante +++	2010-06-11 13:53:46.572	BOURG +++	38	dictionary.result.Levures_Bourgeonnante_+++	69000	\N
700	Y	1/40	2010-06-11 14:35:01.42	1/40	38	dictionary.result.1/40	70000	\N
701	Y	1/80	2010-06-11 14:35:13.198	1/80	38	dictionary.result.1/80	70100	\N
702	Y	1/160	2010-06-11 14:35:23.896	1/160	38	dictionary.result.1/160	70200	\N
703	Y	1/320	2010-06-11 14:35:32.829	1/320	38	dictionary.result.1/320	70300	\N
740	Y	Sanguilant	2010-11-17 09:20:03.11	SANGUILANT	38	dictionary.result.Sanguilant	74000	\N
741	Y	Rougeatre	2010-11-17 09:22:58.382	ROUGEATRE	38	dictionary.result.Rougeatre	74100	\N
742	Y	Jaunatre	2010-11-17 09:26:45.571	JAUNATRE	38	dictionary.result.Jaunatre	74200	\N
744	Y	Absence	2010-11-17 09:31:27.279	ABSENCE	38	dictionary.result.Absence	74400	\N
500	Y	A+	2009-07-31 10:21:31.718		38	dictionary.result.A+	50000	\N
503	Y	B+	2009-07-31 10:33:27.875	B	38	dictionary.result.B+	50300	\N
760	Y	A-	2011-01-13 16:38:57.055	A-	38	dictionary.result.A-	76000	\N
761	Y	B-	2011-01-13 16:39:04.557	B-	38	dictionary.result.B-	76100	\N
762	Y	O-	2011-01-13 16:39:13.848	O-	38	dictionary.result.O-	76200	\N
780	Y	Spores	2011-01-28 14:19:29.289	SPORES	38	dictionary.result.Spores	78000	\N
783	Y	Levures Simple	2011-01-28 14:20:31.926	LEVURES S	38	dictionary.result.Levures_Simple	78300	\N
785	Y	Onchocera Volvulus	2011-01-28 14:28:39.786	ONCH VOLV	38	dictionary.result.Onchocera_Volvulus	78500	\N
786	Y	Loa Loa	2011-01-28 14:28:52.194	LOA LOA	38	dictionary.result.Loa_Loa	78600	\N
743	Y	Présence	2010-11-17 09:31:17.195	PRESENCE	38	dictionary.result.Presence	74300	\N
1060	Y	Positif VIH 1	2012-06-01 15:10:58.561702	\N	38	dictionary.result.Positif_VIH_1	106000	\N
1061	Y	Positif VIH 2	2012-06-01 15:10:58.561702	\N	38	dictionary.result.Positif_VIH_2	106100	\N
1063	Y	Indetermine	2012-06-01 15:10:58.561702	\N	38	dictionary.result.Indetermine	106300	\N
1100	Y	Positif +	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Positif_+	110000	\N
1101	Y	Positif ++	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Positif_++	110100	\N
1102	Y	Positif +++	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Positif_+++	110200	\N
1103	Y	Négatif	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Negatif	110300	\N
1104	Y	Indéterminé	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Indetermine	110400	\N
1105	Y	Positif VIH1	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Positif_VIH1	110500	\N
1106	Y	PositifVIH2	2012-10-24 11:58:24.782378	\N	38	dictionary.result.PositifVIH2	110600	\N
1107	Y	Positif VIH1 et VIH2	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Positif_VIH1_et_VIH2	110700	\N
1108	Y	Positif VIH2	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Positif_VIH2	110800	\N
1109	Y	Positif HIV1	2012-10-24 11:58:24.782378	\N	38	dictionary.result.Positif_HIV1	110900	\N
1062	Y	Positif VIH 1 et 2	2012-06-01 15:10:58.561702	\N	38	dictionary.result.Positif_VIH_1_et_2	106200	\N
1200	Y	1 - PCR to confirm first Positive PCR test result	2020-01-22 21:46:41.469132	confirmPCR	258	EID.confirmationPCR	88200	\N
1201	Y	1 = AZT + NVP	2020-01-22 21:46:41.469132	infaztNVP	281	EID.infantAztNvp	91800	\N
1202	Y	1 = Exclusivly Breastfeeding	2020-01-22 21:46:41.469132	olyBrstFed	278	EID.onlyBreastfeeding	90400	\N
1203	Y	1 = HIV-1	2020-01-22 21:46:41.469132	eidHiv1	279	EID.hiv1	90900	\N
1204	Y	1 = NVP	2020-01-22 21:46:41.469132	eidNVP	280	EID.nvp	91400	\N
1205	Y	1 = Vaccination	2020-01-22 21:46:41.469132	vaccinatn	287	EID.vaccination	90000	\N
1206	Y	1st Line	2020-01-22 21:46:41.469132	line1	158	arvRegime.firstLine	74400	\N
1207	Y	2 - PCR six weeks after stopping breastfeeding	2020-01-22 21:46:41.469132	sixWeekPCR	258	EID.sixWeeksPCR	88300	\N
1208	Y	2 = AZT + NVP + 3TC	2020-01-22 21:46:41.469132	eid.aztNvp	280	EID.aztNvp	91500	\N
1209	Y	2 = HIV-2	2020-01-22 21:46:41.469132	ediHiv2	279	EID.hiv2	91000	\N
1210	Y	2 = Mixed Feeding	2020-01-22 21:46:41.469132	mixedFed	278	EID.mixedfeeding	90500	\N
1211	Y	2 = NVP Single Dose	2020-01-22 21:46:41.469132	nvpSingle	281	EID.nvpSingleDose	91900	\N
1212	Y	2 = Pediatrician	2020-01-22 21:46:41.469132	pediatricn	287	EID.pediatrician	90100	\N
1213	Y	2nd Line	2020-01-22 21:46:41.469132	line2	158	arvRegime.secondLine	74500	\N
1214	Y	3 - PCR after indeterminate result	2020-01-22 21:46:41.469132	indeterPCR	258	EID.indeterminatePCR	88400	\N
1215	Y	3 = Dietitian	2020-01-22 21:46:41.469132	dietitian	287	EID.dietitian	90200	\N
1216	Y	3 = HAART	2020-01-22 21:46:41.469132	eid.haart	280	EID.haart	91600	\N
1217	Y	3 = Never breastfed	2020-01-22 21:46:41.469132	nvrBrstfd	278	EID.neverBreastfed	90600	\N
1218	Y	3 = Other	2020-01-22 21:46:41.469132	infARVOthr	281	EID.infantARVOther	92000	\N
1219	Y	3rd Line	2020-01-22 21:46:41.469132	line3	158	arvRegime.thirdLine	74600	\N
1220	Y	4 = Other(Specify)	2020-01-22 21:46:41.469132	otherSpcfy	287	EID.otherSpecify	90300	\N
1221	Y	4 = Stopped breastfeeding Less than 6 weeks ago	2020-01-22 21:46:41.469132	Brstfedlt6	317	EID.stoppedBreastfedless6weeks	90700	\N
1222	Y	5 = Stopped breastfeeding More than 6 weeks ago	2020-01-22 21:46:41.469132	Brstfedmt6	317	EID.stoppedBreastfedmore6weeks	90800	\N
1223	Y	7 - Not Applicable for First PCR Test	2020-01-22 21:46:41.469132	nonAppPCR	258	EID.nonApplicablePCR	88500	\N
1224	Y	9 = Inconclusive	2020-01-22 21:46:41.469132	inconclsve	279	EID.inconclusive	91300	\N
1225	Y	A cough lasting more than a month?	2020-01-22 21:46:41.469132	cough	244	diseases.RTN.cough	78300	\N
1226	Y	A diarrhea lasting more than a month?	2020-01-22 21:46:41.469132	diarrhea	244	diseases.RTN.diarrhea	78100	\N
1227	Y	A fever lasting more than a month?	2020-01-22 21:46:41.469132	fever	244	diseases.RTN.fever	78200	\N
1228	Y	ACONDA Request	2020-01-22 21:46:41.469132	ACONDA	297	special.request.reason.ACONDA	100300	\N
1229	Y	Adiazine® (Sulfadiazine) + pyrimethamine + folinic acid	2020-01-22 21:46:41.469132	adiPyrFolA	240	arvProphyl.adiazinePyrimethamineFolinicAcid	72200	\N
1230	Y	AES (FR) (AIDs Prophylaxis)	2020-01-22 21:46:41.469132	AES	83	prophylaxis.AES	65700	\N
1231	Y	AIDS Stage A (Adult)	2020-01-22 21:46:41.469132	AIDSStgA-A	84	AIDSStage.adultA	65900	\N
1232	Y	AIDS Stage A (Infant)	2020-01-22 21:46:41.469132	AIDSStgA-I	84	AIDSStage.infantA	66300	\N
1233	Y	AIDS Stage B (Adult)	2020-01-22 21:46:41.469132	AIDSStgB-A	84	AIDSStage.adultB	66000	\N
1234	Y	AIDS Stage B (Infant)	2020-01-22 21:46:41.469132	AIDSStgB-I	84	AIDSStage.infantB	66400	\N
1235	Y	AIDS Stage C (Adult)	2020-01-22 21:46:41.469132	AIDSStgC-A	84	AIDSStage.adultC	66100	\N
1236	Y	AIDS Stage C (Infant)	2020-01-22 21:46:41.469132	AIDSStgC-I	84	AIDSStage.infantC	66500	\N
1237	Y	AIDS Stage N (Infant)	2020-01-22 21:46:41.469132	AIDSStgN-I	84	AIDSStage.infantN	66200	\N
1238	Y	another nationality	2020-01-22 21:46:41.469132	other	79	nationality.Other	76000	\N
1239	Y	Anphotericine B	2020-01-22 21:46:41.469132	anphoterB	242	arvProphyl.anphotericineB	72300	\N
1240	Y	Cachexia?	2020-01-22 21:46:41.469132	cachexie	178	diseases.RTN.cachexia	79300	\N
1241	Y	Cerebral Toxoplamosis : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	CrblToxo	82	disease.cerebralToxoplamosis	64800	\N
1242	Y	Cervical Cancer : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	CervCancer	82	disease.cervicalCancer	65200	\N
1243	Y	Chronic Diarrhea : Demographic Answer to ARV disease question	2020-01-22 21:46:41.469132	DiarrheaC	82	disease.diarrhea	94000	\N
1244	Y	College or University	2020-01-22 21:46:41.469132	PostSecond	240	education.postSecondary	62300	\N
1245	Y	Cryoptococcal Meningitis : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	CryptoMen	82	disease.cryoptococcalMeningitis	64900	\N
1246	Y	cryptococcus meningitis?	2020-01-22 21:46:41.469132	cryptoMen	244	diseases.RTN.cryptococcusMeningitis	78700	\N
1247	Y	D = HIV 1+2	2020-01-22 21:46:41.469132	eidHivd	279	EID.hivD	91100	\N
1248	Y	Dalacine® (clindamycine) + pyrimethamine + folinic acid	2020-01-22 21:46:41.469132	dalPyrFolA	242	arvProphyl.dalacinePyrimethamineFolinicAcid	72100	\N
1249	Y	Dementia or a neurological deficit associated with HIV?	2020-01-22 21:46:41.469132	HIVDement	178	diseases.RTN.hivDementia	80000	\N
1250	Y	Demographic Response N/A (Yes, No or N/A)	2020-01-22 21:46:41.469132	NA	80	answer.notApplicable	64300	\N
1251	Y	Demographic Response No (in Yes or No)	2020-01-22 21:46:41.469132	No	81	answer.no	64500	\N
1252	Y	Demographic Response No (Yes, No or N/A)	2020-01-22 21:46:41.469132	No	80	answer.no	64200	\N
1253	Y	Demographic Response Yes (in Yes or No)	2020-01-22 21:46:41.469132	Yes	81	answer.yes	64400	\N
1254	Y	Demographic Response Yes (in Yes, No or N/A)	2020-01-22 21:46:41.469132	Yes	80	answer.yes	64100	\N
1255	Y	EGPAF Request	2020-01-22 21:46:41.469132	EGPAF	297	special.request.reason.EGPAF	100400	\N
1256	Y	Expulmonary TB : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	TBExpul	82	disease.tbExpulmonary	64700	\N
1257	Y	External Request	2020-01-22 21:46:41.469132	extnlReqst	297	special.request.reason.externalRequest	100200	\N
1258	Y	Extrapulmonary tuberculosis	2020-01-22 21:46:41.469132	expulTB	244	diseases.RTN.expulmonaryTuberculosis	78500	\N
1259	Y	First PCR	2020-01-22 21:46:41.469132	firstPCR	267	EID.firstPCR	88000	\N
1260	Y	Fluconazole	2020-01-22 21:46:41.469132	flucona	242	arvProphyl.fluconazole	72000	\N
1261	Y	General Prurigo : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	GenPrurigo	82	disease.generalPrurigo	65000	\N
1262	Y	Herpes (chronic progressive or generalized)?	2020-01-22 21:46:41.469132	herpes	178	diseases.RTN.herpes	79600	\N
1263	Y	HIV Status HIV-1 and HIV-2	2020-01-22 21:46:41.469132	HIVDual	241	HIVStatus.HIVDual	70200	\N
1264	Y	HIV Status HIV-1 infection	2020-01-22 21:46:41.469132	HIV-1	241	HIVStatus.HIV_1	70000	\N
1265	Y	HIV Status HIV-2 infection	2020-01-22 21:46:41.469132	HIV-2	241	HIVStatus.HIV_2	70100	\N
824	Y	HIV1	2020-01-22 21:46:41.469132		198	dict.HIVResult.hiv1	82400	\N
825	Y	HIV2	2020-01-22 21:46:41.469132		198	dict.HIVResult.hiv2	82500	\N
826	Y	HIVD	2020-01-22 21:46:41.469132		198	dict.HIVResult.hivd	82600	\N
1266	Y	INCONCLUSIVE	2020-01-22 21:46:41.469132		3		9400	\N
1267	Y	Invasive cancer of the cervix or the uterus?	2020-01-22 21:46:41.469132	curvixC	244	diseases.RTN.curvixCancer	79100	\N
1268	Y	Is the patient's mother known to be infected by HIV?	2020-01-22 21:46:41.469132	matHIV	244	diseases.RTN.maternalHIV	79200	\N
1269	Y	IST (?) : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	IST	82	disease.IST	65100	\N
1270	Y	Ivorian	2020-01-22 21:46:41.469132	Ivorian	243	nationality.simple.CI	74000	\N
1271	Y	Kaposi's sarcoma : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	KaposiSarc	82	disease.KaposisSarcoma	65400	\N
1272	Y	Kaposi's Sarcoma?	2020-01-22 21:46:41.469132	sarcKapo	178	diseases.RTN.kaposisSarcoma	79800	\N
1273	Y	Marital Status Divorced	2020-01-22 21:46:41.469132	divorced	78	marital.divorced	102000	\N
1274	Y	Marital Status Single (for ARV)	2020-01-22 21:46:41.469132	celibate	78	marital.single	62400	\N
1275	Y	Martial Status cohabiting (for ARV)	2020-01-22 21:46:41.469132	cohab	78	marital.cohabitating	62600	\N
1276	Y	Martial Status Married (for ARV)	2020-01-22 21:46:41.469132	married	78	marital.married	62500	\N
1277	Y	Martial Status N/A for children (for ARV)	2020-01-22 21:46:41.469132	NA	78	marital.n_a	62800	\N
1278	Y	Martial Status window(er) (for ARV)	2020-01-22 21:46:41.469132	widow	78	marital.widow	62700	\N
1279	Y	N/A	2020-01-22 21:46:41.469132	na	242	answer.notApplicable	72500	\N
1280	Y	No Schooling	2020-01-22 21:46:41.469132	NoSchool	240	education.noSchool	62000	\N
1281	Y	Not applicable (AIDS Prophylaxis)	2020-01-22 21:46:41.469132	NA	83	answer.notApplicable	65800	\N
1282	Y	Oropharyngeal Candidiasis : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	OpharCand	82	disease.oropharyngealCandidiasis	65300	\N
1283	Y	Oropharyngeal candidiasis?	2020-01-22 21:46:41.469132	thrush	178	diseases.RTN.thrush	79400	\N
1284	Y	Other African	2020-01-22 21:46:41.469132	African	243	nationality.simple.OtherAfrican	74200	\N
1285	Y	P = HIV-POS	2020-01-22 21:46:41.469132	hivpos	279	EID.hivP	91200	\N
1286	Y	Pain on swallowing?	2020-01-22 21:46:41.469132	swallPaint	244	diseases.RTN.painfulSwallowing	78600	\N
1287	Y	Primary School	2020-01-22 21:46:41.469132	Primary	240	education.primary	62100	\N
1288	Y	Pulmonary TB : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	TBPul	82	disease.tbPulmonary	64600	\N
1289	Y	Pulmonary tuberculosis?	2020-01-22 21:46:41.469132	pulTB	244	diseases.RTN.pulmonaryTuberculosis	78400	\N
1290	Y	QA Event Type	2020-01-22 21:46:41.469132		36		98100	\N
1291	Y	recurrent infections (otite, pharyngite)?	2020-01-22 21:46:41.469132	recInfect	244	diseases.RTN.recurrentInfections	79000	\N
1292	Y	Recurrent pneumonia?	2020-01-22 21:46:41.469132	recPneumon	244	diseases.RTN.recurrentPneumonia	78800	\N
1293	Y	Retro CI Staff	2020-01-22 21:46:41.469132	retroCStaf	297	special.request.reason.retroCIStaff	100000	\N
1294	Y	Second PCR	2020-01-22 21:46:41.469132	secondPCR	267	EID.secondPCR	88100	\N
1295	Y	Secondary School	2020-01-22 21:46:41.469132	Secondary	240	education.secondary	62200	\N
1296	Y	Sepsis?	2020-01-22 21:46:41.469132	sespis	244	diseases.RTN.sespis	78900	\N
1297	Y	shingles : Demographic Answer to AVR disease question	2020-01-22 21:46:41.469132	Shingles	82	disease.shingles	65500	\N
1298	Y	Significant weight loss?	2020-01-22 21:46:41.469132	weightLoss	244	diseases.RTN.weightLoss	78000	\N
1299	Y	US Embassy	2020-01-22 21:46:41.469132	USEmbassy	297	special.request.reason.USEmbassy	100100	\N
1178	Y	Valid	2020-01-22 21:46:41.469132		245	dict.PosNegIndInv.valid	82350	\N
1300	Y	Widespread Dermititis pruipineuse?	2020-01-22 21:46:41.469132	dermPruip	178	diseases.RTN.generalDermititisPruipineuse	79500	\N
1301	Y	Zona?	2020-01-22 21:46:41.469132	zona	178	diseases.RTN.zona	79700	\N
823	Y	Invalid	2020-01-22 21:46:41.469132		245	dict.PosNegIndInv.invalid	82300	\N
829	Y	Invalid	2020-01-22 21:46:41.469132		198	dict.HIVResult.invalid	82900	\N
1171	Y	Negative	2020-01-22 21:46:41.469132		246	dict.HIV1NInd.negative	86100	\N
1172	Y	Indeterminate	2020-01-22 21:46:41.469132		246	dict.HIV1NInd.indeterminate	86200	\N
1179	Y	VL under ARV control	2020-01-22 21:46:41.469132	null	400	sample.entry.project.vl.arv	1	\N
1180	Y	Virological Failure	2020-01-22 21:46:41.469132	null	400	sample.entry.project.vl.virologicalfail	2	\N
1181	Y	Clinical Failure	2020-01-22 21:46:41.469132	null	400	sample.entry.project.vl.clinicalfail	3	\N
1182	Y	Immunological Failure	2020-01-22 21:46:41.469132	null	400	sample.entry.project.vl.immunologicalfail	4	\N
1183	Y	VL Reason Other	2020-01-22 21:46:41.469132	null	400	sample.entry.project.vl.other	5	\N
1302	Y	the paper form doesn't have any explicit answer.	2020-01-22 21:46:41.469132	NotSpeced	\N	answer.notSpecified	106000	\N
1303	Y	B1	2020-01-22 21:46:41.469132	B1	403		10	\N
1304	Y	J0	2020-01-22 21:46:41.469132	J0	403		20	\N
1305	Y	J15	2020-01-22 21:46:41.469132	J15	403		30	\N
1306	Y	M1	2020-01-22 21:46:41.469132	M1	403		40	\N
1307	Y	M3	2020-01-22 21:46:41.469132	M3	403		50	\N
1308	Y	M6	2020-01-22 21:46:41.469132	M6	403		60	\N
1309	Y	M12	2020-01-22 21:46:41.469132	M12	403		70	\N
1310	Y	Other	2020-01-22 21:46:41.469132	Other	403	labOrder.hiv.followup.byPeriod.other	80	\N
1311	Y	Not Applicable	2020-01-22 21:46:42.070109	NA	317	answer.notApplicable	90900	\N
1312	Y	Unknown	2020-01-22 21:46:42.070109	Unknown	281	answer.unknown	92200	\N
1313	Y	4 = None	2020-01-22 21:46:42.070109	infARVNone	281	EID.infantARVNone	92100	\N
1314	Y	Unknown	2020-01-22 21:46:42.070109	Unknown	280	answer.unknown	92200	\N
1315	Y	4 = None	2020-01-22 21:46:42.070109	eid.none	280	EID.none	91700	\N
1316	Y	B	2020-01-22 21:46:42.174528	B	38	dictionary.result.B	50350	\N
1317	Y	O	2020-01-22 21:46:42.174528	O	38	dictionary.result.O	50450	\N
\.


--
-- Data for Name: dictionary_category; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.dictionary_category (id, description, lastupdated, local_abbrev, name) FROM stdin;
34	VIROLOGY	2006-09-28 09:03:13.541	V	V
35	MOLECULAR EPI	2006-11-08 08:52:56.228	E	E
1	GONORRHEA CULTURE 	2007-10-26 05:15:27.178	GC	GC
3	CLINICAL GENERAL	2007-10-25 16:09:10.239	CG	CG
36	QA Event Type	2007-10-25 10:44:58	Q	Q
37	QA Event Category	2008-10-31 00:00:00	QC	QA Event Category
38	Haiti Lab	2008-10-31 00:00:00	HL	HL
77	intake status	2010-10-28 06:10:34.12594	REC_STATUS	REC_STATUS
117	Conclusion	2011-02-16 12:45:59.612333	Conclusion	HIVResult
137	specimen reception condition	2011-02-16 14:46:31.952827	reciptCond	specimen reception condition
157	haitiDepartment	2011-03-29 15:37:16.61796	haitDept	haitDepartments
177	patientPayment	2012-03-13 15:32:16.552198	PP	patientPayment
197	possible language locales for the user to select	2012-05-14 11:24:08.802607	locale	locale
217	Possible education levels	2012-06-11 09:33:46.926535	education	Education Level Demographic Information
218	Possible marriage status	2012-06-11 09:33:46.952621	marriage	Marital Status Demographic Information
219	Possible nationalities	2012-06-11 09:33:46.961175	nation	Nationality Demographic Information
237	General test result	2013-11-07 10:00:30.201815	Test Reslt	Test Result
257	Reasons for rejecting test results	2014-05-01 20:40:55.81263	resReject	resultRejectionReasons
277	Programs for orders	2020-01-22 21:46:11.052657	programs	programs
240	Education Level Demographic Information	2020-01-22 21:46:41.455994	Education	Education Level
78	Marital Status Demographic Information	2020-01-22 21:46:41.455994	Marital	Marital Status
79	Nationality Demographic Information	2020-01-22 21:46:41.455994	National	Nationality
80	Yes No N/A question choices	2020-01-22 21:46:41.455994	YesNoNA	Yes No NA
81	Yes No question choices	2020-01-22 21:46:41.455994	YesNo	Yes No
82	ARV diseases for demographic question	2020-01-22 21:46:41.455994	ARVDisease	ARV Disease
83	ARV Prophylaxis treatments for demographic question choices	2020-01-22 21:46:41.455994	ARVProphyl	ARV Prophylaxis
84	AID Stages demographic question choices	2020-01-22 21:46:41.455994	AIDSStg	AIDS Stages
97	Simplified list of nationality demographic question choices	2020-01-22 21:46:41.455994	RegionNat	Regional Nationality
241	possible HIV status for demographic question choices	2020-01-22 21:46:41.455994	HIVStatus	HIV Status
242	Secondary ARV Prophlaxis choices for ARV studies.	2020-01-22 21:46:41.455994	arvProph2	Secondary Prophylaxis
243	Simplified Nationality Choices for demographic questions	2020-01-22 21:46:41.455994	NatSimple	Simplified Nationality
158	ARV treatment regime choices for ARV studies.	2020-01-22 21:46:41.455994	arvRegime	ARV Treatment Regime
244	Diseases for Routine Tests	2020-01-22 21:46:41.455994	RTNDisease	RTN Diseases
178	Diseases for Routine Test Examine Questions	2020-01-22 21:46:41.455994	RTNExam	RTN Examination Diseases
245	Result Values - Pos, Neg, Indeterminate, Invalid	2020-01-22 21:46:41.455994	PsNgIndInv	PosNegIndInv
198	HIV Result-HIV1, HIV2, HIVD, Neg, Indtrminate, Invalid	2020-01-22 21:46:41.455994	HIVResult	HIVResult
246	Result Values - HIV 1, Negative, Indeterminate	2020-01-22 21:46:41.455994	HIV1NInd	HIV1NInd
267	EID Which PCR Test	2020-01-22 21:46:41.455994	whichPCR	EID Which PCR Test
258	Reason for Second PCR Test	2020-01-22 21:46:41.455994	rsonPCRTst	Reason for Second PCR Test
287	EID Type of Clinic	2020-01-22 21:46:41.455994	typOfClinc	EID Type of Clinic
278	EID How Infant Eating	2020-01-22 21:46:41.455994	howEating	EID How Infant Eating
279	Mother's HIV Status	2020-01-22 21:46:41.455994	momsStatus	Mother's HIV Status
280	Mother's ARV Treatment	2020-01-22 21:46:41.455994	momsTrtmnt	Mother's ARV Treatment
281	EID Infant's ARV Prophylaxis	2020-01-22 21:46:41.455994	infantARV	EID Infant's ARV Prophylaxis
282	EID Infant Cotrimoxazole	2020-01-22 21:46:41.455994	eidCotrim	EID Cotrimoxazole
297	Special Request Reason	2020-01-22 21:46:41.455994	SpeclReasn	Special Request Reason
298	ARV Demographic Marital Status	2020-01-22 21:46:41.455994	ARVMarital	
299	RTN Demographic Info: Nationality	2020-01-22 21:46:41.455994	RTNNation	
305	ARV Demographic Info: Nationality	2020-01-22 21:46:41.455994	ARVNation	
317	Choices for when an infant stopped breastfeeding	2020-01-22 21:46:41.455994	stopFeed	EID Stopped Breastfeeding
400	Reason for Viral Load Request	2020-01-22 21:46:41.455994	vlReason	Reason for Viral Load Request
403	Test Location Codes for CI LNSP	2020-01-22 21:46:41.455994	LocatCode	testLocationCode
404	ARV Demographic Education Level	2020-01-22 21:46:41.455994	ARVEd	
57	Demographic Yes, No, Unknown responses	\N	YesNoUnkn	Yes No Unknown
\.


--
-- Name: dictionary_category_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.dictionary_category_seq', 420, false);


--
-- Name: dictionary_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.dictionary_seq', 1319, true);


--
-- Name: district_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.district_seq', 1, false);


--
-- Data for Name: document_track; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.document_track (id, table_id, row_id, document_type_id, parent_id, report_generation_time, lastupdated, name) FROM stdin;
\.


--
-- Name: document_track_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.document_track_seq', 60, true);


--
-- Data for Name: document_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.document_type (id, name, description, lastupdated) FROM stdin;
1	nonConformityNotification	Non_Conformity reports to be sent to clinic	2011-09-26 17:32:05.871573-07
2	resultExport	Results sent electronically to other systems	2012-01-31 15:16:20.902841-08
3	malariaCase	malaria case report sent	2012-05-01 09:46:34.394366-07
4	patientReport	Any patient report, name in report_tracker	2012-07-13 15:05:22.902547-07
\.


--
-- Name: document_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.document_type_seq', 4, true);


--
-- Data for Name: electronic_order; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.electronic_order (id, external_id, patient_id, status_id, order_timestamp, data, lastupdated) FROM stdin;
\.


--
-- Name: electronic_order_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.electronic_order_seq', 1, false);


--
-- Data for Name: gender; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.gender (id, gender_type, description, lastupdated, name_key) FROM stdin;
145	M	MALE	2006-10-10 13:18:40.094	gender.male
146	F	FEMALE	2006-11-21 12:04:02.372	gender.female
\.


--
-- Name: gender_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.gender_seq', 1, false);


--
-- Name: hibernate_sequence; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.hibernate_sequence', 4819, true);


--
-- Data for Name: history; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.history (id, sys_user_id, reference_id, reference_table, "timestamp", activity, changes) FROM stdin;
1	1	0	0	2014-06-27 00:20:35.362	T	\N
2	1	10	184	2014-06-27 00:20:49.227	U	\\x3c76616c75653e747275653c2f76616c75653e0a
\.


--
-- Name: history_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.history_seq', 20, true);


--
-- Name: hl7_encoding_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.hl7_encoding_type_seq', 4, true);


--
-- Data for Name: hl7_message_out; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.hl7_message_out (id, data, lastupdated, status) FROM stdin;
\.


--
-- Name: hl7_message_out_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.hl7_message_out_seq', 1, false);


--
-- Data for Name: htmldb_plan_table; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.htmldb_plan_table (statement_id, plan_id, "timestamp", remarks, operation, options, object_node, object_owner, object_name, object_alias, object_instance, object_type, optimizer, search_columns, id, parent_id, depth, "position", cost, cardinality, bytes, other_tag, partition_start, partition_stop, partition_id, other, distribution, cpu_cost, io_cost, temp_space, access_predicates, filter_predicates, projection, "time", qblock_name) FROM stdin;
7146420525171748	1	2008-05-08 14:33:14	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146420525171748	1	2008-05-08 14:33:14	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146722603172428	2	2008-05-08 14:33:20	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146722603172428	2	2008-05-08 14:33:20	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146420525171748	1	2008-05-08 14:33:14	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146420525171748	1	2008-05-08 14:33:14	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146722603172428	2	2008-05-08 14:33:20	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146722603172428	2	2008-05-08 14:33:20	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146420525171748	1	2008-05-08 14:33:14	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146420525171748	1	2008-05-08 14:33:14	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146722603172428	2	2008-05-08 14:33:20	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146722603172428	2	2008-05-08 14:33:20	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146420525171748	1	2008-05-08 14:33:14	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146420525171748	1	2008-05-08 14:33:14	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146722603172428	2	2008-05-08 14:33:20	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146722603172428	2	2008-05-08 14:33:20	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146420525171748	1	2008-05-08 14:33:14	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146420525171748	1	2008-05-08 14:33:14	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146722603172428	2	2008-05-08 14:33:20	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146722603172428	2	2008-05-08 14:33:20	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146420525171748	1	2008-05-08 14:33:14	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146420525171748	1	2008-05-08 14:33:14	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
7146722603172428	2	2008-05-08 14:33:20	\N	SELECT STATEMENT	\N	\N	\N	\N	\N	\N	\N	ALL_ROWS	\N	0	\N	0	238	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	\N	3	\N
7146722603172428	2	2008-05-08 14:33:20	<remarks><info type="db_version">10.2.0.1</info><info type="parse_schema"><![CDATA["CLINLIMS"]]></info><info type="plan_hash">288471584</info><outline_data><hint><![CDATA[FULL(@"SEL$1" "CITY_STATE_ZIP"@"SEL$1")]]></hint><hint><![CDATA[OUTLINE_LEAF(@"SEL$1")]]></hint><hint><![CDATA[ALL_ROWS]]></hint><hint><![CDATA[OPTIMIZER_FEATURES_ENABLE('10.2.0.1')]]></hint><hint><![CDATA[IGNORE_OPTIM_EMBEDDED_HINTS]]></hint></outline_data></remarks>	TABLE ACCESS	FULL	\N	CLINLIMS	CITY_STATE_ZIP	CITY_STATE_ZIP@SEL$1	1	TABLE	ANALYZED	\N	1	0	1	1	238	79529	5726088	\N	\N	\N	\N	\N	\N	33895495	232	\N	\N	\N	"CITY_STATE_ZIP"."ID"[NUMBER,22], "CITY_STATE_ZIP"."CITY"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE"[VARCHAR2,2], "CITY_STATE_ZIP"."ZIP_CODE"[VARCHAR2,10], "CITY_STATE_ZIP"."COUNTY_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."COUNTY"[VARCHAR2,25], "CITY_STATE_ZIP"."REGION_ID"[NUMBER,22], "CITY_STATE_ZIP"."REGION"[VARCHAR2,30], "CITY_STATE_ZIP"."STATE_FIPS"[NUMBER,22], "CITY_STATE_ZIP"."STATE_NAME"[VARCHAR2,30], "CITY_STATE_ZIP"."LASTUPDATED"[TIMESTAMP,11]	3	SEL$1
\.


--
-- Data for Name: image; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.image (id, description, image, lastupdated) FROM stdin;
1	headerLeftImage	\\xffd8ffe000104a46494600010101006000600000ffe1002c4578696600004d4d002a000000080001013100020000000a0000001a00000000477265656e73686f7400ffdb00430007050506050407060506080707080a110b0a09090a150f100c1118151a19181518171b1e27211b1d251d1718222e222528292b2c2b1a202f332f2a32272a2b2affdb0043010708080a090a140b0b142a1c181c2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2affc000110801bf01ee03012200021101031101ffc4001f0000010501010101010100000000000000000102030405060708090a0bffc400b5100002010303020403050504040000017d01020300041105122131410613516107227114328191a1082342b1c11552d1f02433627282090a161718191a25262728292a3435363738393a434445464748494a535455565758595a636465666768696a737475767778797a838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae1e2e3e4e5e6e7e8e9eaf1f2f3f4f5f6f7f8f9faffc4001f0100030101010101010101010000000000000102030405060708090a0bffc400b51100020102040403040705040400010277000102031104052131061241510761711322328108144291a1b1c109233352f0156272d10a162434e125f11718191a262728292a35363738393a434445464748494a535455565758595a636465666768696a737475767778797a82838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae2e3e4e5e6e7e8e9eaf2f3f4f5f6f7f8f9faffda000c03010002110311003f00fa2a8a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028accf106a7fd91a449744e3690338cd68c6dbe246fef283400ea28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a2b374bd4bed535cda4ccbf69b67c3ae7b1e41c7d315a54005457300b9b69212cc9bd48dca7047bd4b4500729a3eb573617eda4eb32891d5f6a4bd0e0f2b9fc2babae1bc52889e23dcca0a7906571dce081fd6ba6d1f51fb5ac90487f7d09e47aaf635cd4eba755d27ba2dc6c9334e8a28ae920e2be274fb7c3a22dc540612b63a903ff00d75d569572b77a4db4e872af1822b95f1dac73cc227e42da4848fc456ef84d94f85ec55483b63c1c76e6b0854e6a9287629ad133628a28adc90ae7343d724baf11ea7a6cc3e58583c2deabc0c7e79ad4d63525d36c4b8f9a673b224079663d2b94d023fb3ebd0be4b19998061fddc13cfe3584eb4635234fab2946e9b3baa28a2b72428a6c922c51b3c8c15546493d8566687acc7ae453dcdbe7c8590a4671f780ea7f3cd006ad1451400514514005145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514005150b5ddba4be5bcf1abe71b598026a6073d2800a28a280383f124f3787fc7167a9c200b7b84db719ff003ed5dcc134773024d03878dc65587422b9df1cd92dce88b337ddb7903b6072474c7e6693c277d2088d95cb6eda7e427d7bafe158fb451a9c8faec572fbb7474d451456c49c6eb3025df88af79c98edfcb61e99c1fe9563479f66a9692823fd26331c840fee8247f3aab2dcc6fa8eaf367991d235e3b85c7f4ae6341d464177621dd8b79b8503b64e2be7e2e52c73927b3b7e075dbf747add14515f40721c378a02c971abcdbb261851473d32bcff002adbf09161a74d136dc452ed1b7d3683fd6b99d6e6f3b46d7ae871bc94ff00be722b53c0ce6392eedd89f9f13631eb81fd2bc8c34f9b1553d7f2b1bcd5a08ec291d82233b1c2a8c934b595e269dadf409d97ab154ffbe980feb5eb3765730dce76f6ee4d52e1afd570518c3688dd09fef7e82ae6896c3fb7c2007cbb5b7fbd9fe32791f91aa2b3db596a51c73bec8ed210ab9f71ff00d6adef0bc0469ef78f9f32edcc873dbb7f4af0706e788c4bab2d97f48eaa968c3951b745155efef23b0b196e6638545cfd4f615ef9ca723f1175d7b5d3d74ab361e7dd03bce7ee2773fad6ff0085b4d1a5786aced70bb9532c477c9cff005ae12cad64d5f5e7bfbcdccf348b181fdd0c7e61ff0001af508d0471aa0e8a00aca9d4552ed6c3946db8ea28a2b510514535240ecc07f09c1a0075145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514018fafe8106ad6db954477519df1caa3073f5ac3d3756beb29143ee7504acd6ee72ca47deda7a9fa57695c9eb766575d611f0d731ee8f8e15d79cfe3915c78b9ca943dac7a7e46904a4eccea2dee22bbb759a060c8c38352570fa178822b6d592d5be44b86daca7b38ee3d8e2bb8ade8d55560a489945c5d8af7f6eb7761340e321d08c579c5add5cd8b4397f9dbf7593da65e4fe7902bd3ebcfb5eb02752d56d003f745e43fefe49207fdf22b8f1ead1551746694b576676da65f0bfb08e6046ec61c0ecc383fad5bae27c37ab155b69c6d115ce12e3fd890703f406bb6aeba1555582923394795d8f3bd47fd0fc41736ecdc2ccb273dc15273fad50d2b48924d156753b6681c4b8039f94e48fc715d178e34e68e5b5d620eb0b6c98638287b9fc8536d1fec3a818d8016f7437a3f6dddc7f2af9fc6a9e1eb3947aebf71d74ed28d99d5d85dc77d6315c44c087504e3b1ee2a59a4f2a0790f4452df90ae66de61a16a65ceefb1dd300c0748dbd7e9d6b735897cbd12edd7fe78363f235ef61f111af495489cb38384acce0b54de3c03a948c41691e5901f62c48ad4f094ac757b7207c9269a993efbdaa87880b47f0de520e18daa9fc48a8bc237127dbf4562c30e8d1363be149af0f012bd6e6ef27f91d5553e53d2ab9df17cc5aded2ca3237dccc38cf385f9bfa574124891465e46545032598e00ae3d6e4eafaa49ab4a36da5b295b707f8bd5ff98af5f1d5d51a2dbdde88e6a51e692388f1c5e3c57973e4e48910a9c0f4af5dd1ca9d1ad0c78dbe50c62b8b8b4a86fed16ee584334933364f742735d67868b37872ccb72769ebfef1ae4caea2945c17434aeacd3352b8af14eacb7570f0c59682cffd61ecd29e02fe1906b7fc47aa3e9ba5b7d9b06ee5f9205f563d2b8e5b659b58b3d3d092b6ca6e2e5cf77e783fa56f8eafc91e45bbfcbfad08a51bbbb35340b361ad5ac3211bed202f3e0f591c039fcc1aeceb03c2c8664bcbf7cee9e7655247f029f97f9d6fd74e1a1c94a29933779051556fb51b5d36032ddca100e83a93f41d4d7317fe22bebdb766831a6da7fcf793efb0ff00647f88aaab5a9d2579bb0a3172d8b9e30f137f6369f241603ced4245c4718e76e7b9a7f81ae6f2f7c2f0dcea5ff1f12162c718ee457271d84b72cb27ce45c36c843ffac94ffcf46f40320f6af46b0b54b2b18ade31808b838f5ef5142b3ac9c92b21ca3ca58a28a2ba480a28a2800a28a2800a28a2800a28a0107a1cd0014514500145145001451450014514500145145001451450014514500145145003652e23262019bb026b323f11d835e35a4d218274c02241804fb56ad66ea3a35a6a0db98049c0e245eb8a9936968356bea4b0dfe6f9ed661b4e7f76dd9862aed7117567a8e8539923265b65c1c67fd5f3d477ae92df50f32f6100a94b88839ff64e0607e39ac6955e6ba7a58728d8d3a2991cc2479147543834d9e7f2800a373b1c2ad74124b556fefe3d3adcdc5c6442bf7d87f0fbfd2ad0e833d69b2c493c4d1caa1d186194f7140090cd1dc42b2c2c1d1c655877a7d703aa5d6a7e08d504e9bef34a9be548863f727aff008fe75d5e8dafd96b70eeb56dae3ef46dd4524d01a7589e210d1c967723a249e59ff816056dd66f882d1ef3469921e654fde20f561c81595787b4a528774545da499e69e288bc8d71fecf20cb01200bc6c2302bd17c35ac2eada4c65e456ba8c059d41e8dfe15e6a6d6e6fb7cc5496604862320303822ba1f0e5abcba72dcdbc86d2f55b0e17f507dabc5c3e2e3858f24f55b1d7529f3ea8f41ae63c5abf64bbd3b5255c8497c9931dc3e1455bb1d7dc5d0b4d5a358243feae507e493fc3f1a9bc4f61fda3e1dbb85465d50c91e3fbca323f5af5e7c989a2d45dd33955e32d4e2ac9174af135ce9130ff45bf0658b27f8bbfeb9aedb42d4bed70bdb4e7fd26dced719fbc3b37d0d707ab17d43c2b67abdb645c59e0487b90bc30fceafe9faafd9fcad4edd03328513203d633c027e9c9af17058874a769ecf47ea8e9ab0baba3bdbab74bbb59209943248b820f7ae1cdbb4fe1dbbb395bf7d60cca181e7728dc2bba86549e149623b91d4329f635c5e905e5d5bc40b201b3edcea07a8da2baf364bd8a9f54ccf0efdeb18fa6f883fb574892d2f8e1da3c0933dc7f5ae8f50d4ccfe00b7ba7608d71e5a7073d5c2d79ce90c3edb3472e3cb5bb900f6f98f15d95d8dde11f0f581c833c808ff00801ddfd2b2c1da9c2ab5b6ff0099a555cd28878d53ecfe03bb41fc10ed1f80ae7bc3170534dd22e5cffa9bb2bc7fb4a07f5ae93c7ac57c1b7b8c7298e6b86d0e563e18058fcc97509c03d3e75cd70606568a97f78d6a755e47a578a666d4756b4d0d4b08245335c1538c818f97f1cfe959fe25bd8acace0b043e5aca40623f85473fae3156aedffe2bc56fe19ad772fb600ff1ae5bc4d23cfaf4d29ff551010b03d383bbfad698e72a98ce596c85422943d4e92f755822d08c7a79fdf34612041ea47ca2badb0896cb4b8518e0471e589eddcd79d783626d57c40a4e0dbda9f3187fb5fc1fa66ba7f166a723cb0e8760717177cc8e3fe59c7dcfe38c7e35e8e069470d46552473d6939cec8cd97514bfbcbad72ef8b3b10cb6e33c1c7de6fc702b2746ba9a2d1f51d4a6c6fba6c671ce58851fa6293c5f709656965a259908870d263b2af63f5cd5b8613243a0e968302ea532ce31fc201c1fcd4579d172c4d4e77f69fe08d9fb91b763b9d1ecdac347b5b6724b471aab1273938a8f5ed620d0f4996f2e0fdd1845eecc7a015a35e47f11bc45fda3af43a6da93f67b370667ec1b1d7f235f49397246e71c63ccec6f5c5f243610ea9abedb9bc98030c4470a4f603fad643dfbea17d1c6146a3a831cc7020cc500f53ebf5c567ea4d3ddd94176accca418a04c761dff001cd7a2784bc370687a6a39c49753286924239e7b578986c3bc44dd4a8eeffad0e99cfd9ae5896344d1a5b4ff004ad4a459ef9970580c2c63d17fcf35b34515eec62a0b963b1cadb6eec29b24890c6d24ac151464b1ed591aef89ac742b7669dbcc9074890f3ffd6ae12c6ff5bf889af6d7dd67a2db37ef02f1e67b536d08f4db4bd82fa1135abf99130cab8e87e953d456d6d159db25bdb208e28c6d551d854b4c028a8ee0b881cc670c0645316e01b2f381070b93f5ef4a52514db0dc8e6d42383cfddd215dc4fa9f4a963bb8dedc4a5828d81981fe1c8cd79a58ebf26ab713acd118d2e2e0c819b2772261b803e86a9dfeaf3eab3b461e6b4b37720e7ac8a0e07039e9d2b8a55e4968b566aa09b3bdb5d707882f26b6d29b36b09db2dca9effdd1ef5bb0c4b044b1a6703b93926b27c3166965a34714367f648f1f2a1fbc7dcd6cd75c2f6bb337b8514515620a28a2800a28a2800a28a2800a28a2800a2abdf5d3da5a3cb140f70e3a471e327f3ae075bf116ab130b9b8d22ea15cf1e53aee03e84d44e6a2ae528b67a1cd3476f0b4b330545ea4f6aa57bacdb59d8ade026580b00cf1f3b41ef5c45a6b1ad41b49ba6983283f65bb404aae7fd81fd69d7b7105e5bcd71653416aaa764e712100f718c11dab9e5898a575f932fd9bbd8ee8ea317909709f3c0dd5979c53c5f406444df8f33ee3766f6ae5349d627999bec8f69716fb7e7550e067ea702b36db52bd8d750b2beb376b3662f04f1303e51249f5aca38c4dd9bb7a95ec99e8b54afec527532ef96391070623827fc6b89d3bc769159dbc37e5848e4ab2ba9053f1e98adaf0df89d6f2ea7b0bb60b2265e139cef8fb1fcb15b46bc2abe568870712b6aff6f5b6668b53b6bbb5fbb242e029fcf3d69b1662b8d267b767f29e4447de318abfaf69d697d6c278ed0cea73f3c2c4103d719c570d6fa8a695e7dade5d992da0904919704328e727a76ae79d469f2f66bee2d46eae7a26877f16a1a86a1240fb9125d83f21565e6cea8d3b385b7b742a58f424fbfb62b96f0d6a16d61e188846c82eee419d949e4e09fe82a85f6b6baba81712359fda240115549c01df033d735b4ebaa514ba90a0e4cec2e7c53a6d9c3e65c4bb0336231ddc7a815622d7b4c9205945e440363866008fad70d1e916ef73e65e6dd5403c221285063b671f5ad1d3fc2b637572d79a7dc1788b61e394ff00aa61e98f4a9fac35aee3e43afbcb4b6d5b4f78260b24520ea39fc6bcea4b0bcf0deb2d1a16c904c6e0f2ebedfed7a8ef5da7f644f62c93e9d2e187fad8b395907af3d0d4fade931eb7a5989dbca97ef4527746ec6aa4a55a3a2b7664ab2650d23c5114b046b7ae3d0cd8c007d1bd0d744ac1d4321054f208ef5e4337db34bbe9a4318124476dd5b93c4abfde1fa5749a26b8d05a0b9d3cbdd591c6fb5246f83d6b0a58de57c95b4f3ff32dd37bc4d1d5349b9d2ef5eff4a8fcdb690e6e2d8751fed28fe9548b075fed3d100639c4d0f4ddeb91d9abaeb2bfb6d420125ac8ae3b8ee3d88ae6fc4962da34bfdb5a72e1338ba887465fef63db935863b00aa27569eff9974aadbdd663cfe26d335185ed6f23656079c8fba7d7eb5d37853551a869cd6f2cde74d07ca4b7565ec4feb5c4f8ab4b86e2d1757b1014150cfb7b823ad67f833599b4ff001243e62ed8a41e5cfdf24f0bf864d73e5938c1fbaf47baf336af06d5ce9f49b5fb2ea5ace873f31893cc881eeae371fd4d72f6535c691793e9920dc2d9c8da7aca87827e95d7f8a31a6f8d349d4d48586e94dac847424f20fe4b5cf78fad058dfdb6b280f972620b823b2f6fd49ac3134d52c54a1d25afcc29be6826755e15d5caac965712ab215325a9cf55ee3f02715cdd8789ede0b1ba951b32dc5c348dedd3fc2b06e2e26934992286e3ecd7318f32d64c8010e385fc41cd71ba06a41ec23b78e1b8b89f66182a1e5bebd2ba2a4beb141425ba128f24db3d013c3534fa10bb8dc8924b9794edebb4b13fe15d05bdd0d427f0d46bcf9113c8f9ea32a47f4aa9a56a7af49a7c3043a0b42150296b86183818ec735574df08f88acf5cbad446a16b1f9e30b12862231e8322b8a18871a752136b5d8d250e6716ba1a9f11982f822f33dc6066b80f0ecbbf42951d4104b37fdf0bbbfa576bae784358d7f4d7b2bcd6c889c8242a8ff0ac7d3fe186a7a647b2df5a122e1c159578f99769e83d2a28d4a74e972b96b72da6d9bfe1bd413c457d16a76ae24b6b7b48e247073962a378fc08aaf66d6fa96b1ae69f72abe679e4c44ffba2a4d134bd6bc2da2c7636b6967711c5d3c92c198fa9cf15c05d4de21d27546bdd474bb981ddf73c8b86079f627b55ca5f58ad39dd6bb1315c9148ef3c1f7d0785e5d78ea8047f6744919bbb000f4ad3d122936ddebfaa1c4f740be0ff00cb38c7403f2cfe35c269f2bf8c7e2324bbfcbd3e18229278dbe5f31c0fbb83f8d74ff11758161a243a65ab6d9ef9c449b7f854727f0c022b6c4e2255610c32d1f52230b49cce656e9f5cd6ae2fe56c4771279709f54078c7e15e81e192ba978b2f2e23f9a0d3e15b54f40dc31ffd0ab86d22282c9c13b4456aa048a4f6ee47e55a1e08d56e355d165b6d15c40d712b4975727af5db85f7c015d38795384dd4968a288aa9b4a2ba9deebdaf9859b4ed24897517e3039110fef35701e23d12cf4ab7853717b9b96c3963cb927279adfbed4f49f075811feb6e5b90a0ee790fb9ae23fb5af357d696eee46e7770b1463a44bd78ac6a62aa6267ceb482fc4aa74d415ba9dc8d3a333e85a645d23c3f1dc26335df018000ed5c9786236bfd5e5bfc9682dd3c88988fbc7a31fe55a9ac7896cf49c44b9b9bb71fbbb78b9663fc87e35eae021ecb0e9cfaea73557cd3d0d59ee22b585a6b891628d4659d8e00ae435df17c8b0b7d888b6b6e9f6a93abfb20eff5cd64eafa8bc6bfda1e2694163fea34d88f43efeff8e2b9f55bcd53528ee6fe2324d21db67649f7507a9f6ac2a639cdf251fbff00cbfcca8d1d2f225b7b1b9f106a51dbc218b4c0339739655fef39f5f407d2bd534ed32d746d256d22f96345f9989c64f7359ba5e8b369761f66b2921fb5b733dcb75663d7159da95be916d7221d6b509efef3aac0495dc7d3e5c0fcebae8d374d5a5f3667295de83352f142e9f7c34e7b91b26c98261267a74c9eff004ad8d23c43f6ad0d6eee93632b946cf19c1c647e558fa9ce89a6a19e2d3ede1521a28e45677523a676e6b1acd7cdf0bea90c17605c4a4cd0aed2151b3db233eb59b9f2ddc65a798d46fba3d3239524e158138c900d60ebf72ba769176b6df23203263b107afea6b0fc2faffda66d1a398b19a48584adfde20545e2ad4606f122dabb029790b42872707919fd4554aaf353bcb6bb4fd351a8fbd6453556dd6567a3ec49ed6db65d5d11910039ce7df06ba1d0b4dd2ece68a58a37bc99d82fdb2e392c7fd927b57250ce2294c30958b46493cd96e59bf7974e3f847b703f3aec13c416cd636325cc3f64c4e0f9647dc5c1da4fd452a75a32f79e9ff041c1ad0e96e2e16dd016e4b1000f5a825bf0ba8c566832eca5dcf6551ffebae0ef3c6ee2de7d74c5bed233e559c6a0e64763b7fa8ac4d7fc74d613d859492476da96a003dc3925842a071d33d41adbdacdfc289e54b73d61752b630cd399944509dacc4f00d3ac6f05f426645c444fc8d9fbc3d6bc1359f199d5b5a8749b5f320d32d981c29c19c8e7249ec6ba9d2fc7c9aadf2697a8dcae970c78548a28db7480718248a9f6ce3b872df63d6229e29b779522bec3b5b69ce0fa53ebcd6f7c6b2431983405821b484e2491f3bc0f503bd4ba57c49b492f16cac74dbebc9db1e64acc8bb8fe2455c31119bb20953715767a2d155acaee4ba4dd2dac96fc74720ff23566ba37330a28a2800a28a28032f5dd5c69565ba301e793e58933c935c8dcaea51431ff0068bbcdaadd65a303a40be8a3d7a576aba55bff006835e4a3cd94fdddfc851ec2aac905a69d7725fdcb34f3b70a48ced1e80572d452de4ecbfafc59a2b74466ac6344d396c2cf7dcea53afef24cfcd93fc4c7fcf4a82d74e5bbb4fec6b576fb3e4b5dcc9c6f6272c01f5c9351dd78ab4ad2a69669acae95e67f9a4c027f9f4ab10ea71ea16725c5adc4fa6443bba28ddee2b96a36a5ccf6febfaf3358c74b20b9d3d23478a489adb4e83eedbc03e698fa9c573f75e1dd6ae6dde54ba8b44b4627e5893e675f7191cd5f6f124969a8adb9d5a2bccb60a4a9cfe1b45636b3e2a65d70c5a56a56b03af2f0b877cfd7838ae78d4b3e65f8a6534dab7ea416c567ba7b06b3bcd46d554a99a705d73ebbb1c564344961e2db4bad0ef4c5736e429b491f21978f954fe1d2b7efafe7bbb55b8b4b186695cfef3ecf2b287fc091cd711e2e8ae6dad0ea31d80b231b0cc6198bf3df3cfa7ad651e5f68f95d9bfeb634f7b9755748f408fc62b6735ddb47332032214078310620723eb9a6dddfd96b936a1a36b5047fda1e53a5bdc6cda6453e9fa579bf85f5687c52ab6da94452e97a4a0e0c8073835bc74dbad2fc6da3c2c5e488dc4650bb65915b92b9ef8e2adce77943faf90b9524a453d4b5984dab5fab081fcdf26dc31c0518c6efcf22ba5f0c5c4f691acb7573752b6cf9ae059b30503b6ecf22b8e8a04bbd4f64de5c70db8280b7277ef270a3b9e7bd6fe937faa2433aea865b411c9b208e60a3cd5f4c0fc2adc95b9989c7a23d06531be9f15eeb9246b1c52660755dad2823d3b75ad5b5feced3a1172ec968b70377947e527f0f5af3cd63577b8b9b0b9b811dc9b71b4da92439ebce071dea6ba96e6fd06a3a769c2558b89209a46de98f419c62aa4e36bcb65b2e9f3feac66afb23d3edeea3bd8f7c3e6051dc8c53e49602a55c87da37151c9fcabcf676b8b0b4b1bf98492d8ca70c15c8309e7ae3b66b652780116ba9abc4b21dd6f78a4ed61dbfc9ae88d6ad1769aff00233718ee992f8a7c3b0ebfa60b9d30aade42331b2ff10eea7f5fc6bcd61b9bbd1eedb50d3c345206d9736edfc38eaadfd0d7a8db69d716170d241a82142b94dc7fd67fbdd87e1587e34f0bcd7962759b0882de2a66e214fbb281e9efd68c461fdb479a3bfe63a75395d995f4ebd8b5984ea3e1b9fec97e9ccd01380e7fda1fd6ba6d335f83598e4d335584daddb29492073c38c73b4f715e18b7f77a65d477fa63b4722b73e8dea08af4ad1359d3fc6da6813afd9f5087ef2a9c321f507b8af369e22a613496b0fc51bce929eab723b0beb7b0b49f49bd994b452bc607a02491fa62a7f0be9966d61776b20065526190e72594746fd4d703e31b2d73c39aa4f797bbef6cee08ff0048551f26060671f4acff000e78e4e93789789ba63261190124cbedf519ae6a987f68a53a2f47a9b29e8948efbc69a85ce97e0f9ad7558e4916d8892d2ed06ec1070037a707ad36ff005dff0084c342163a1da35d3cf18dd338c4719ec73dc8eb5722d2b50f1884b8d7d4db69c7e64b11d5fd0b1ff035d65a595b585bac3670a431a8c05418ac2be29cd47da6b25d421051db638ad0fe19dbc16b17fc2417526a12ae0ec271183f4e735d95969b65a6c423b1b68e0403188d7156a8ae2a956751fbccd124b60a28a2b218514138eb480e45002d046460d14500655d78734dba90ca2dc4331e7ce87e57fceb87f11f83b5b8b578f548273aa450a6c10ca7e651cf4eb93cd7a6d15bd3ad383d04e29bb9e2ba8dfc09e1dbc849315cbe2dc44e30e03e73c7b62aee81a92787f4a4fb301e663646070327bfd2bbef1178374af11c045d45e5cdfc3347c329f5f7af2fd6349bcf0cea115bea68d25b7dd82e1390467383efd6bd2a5529d68f275ec459a6760746b7bcd1ff00b403fda2ee423cd95cf1efcf6155743d18ddc8cc8eb6f6309cdcdf3f008feea1febf5aaba86b8b6a74fd22deda5baf3d3cc92087ae38c293db3939aeb6cf45b9bf11cdad958a1451e5d8c3c228f7f53574a54e947da5677ecbfae829b937cb1fbc94ea7757b0a699e13845a69f18da6f48ea3fd91dfeb9acad5356d33c1b0b456a0ddea938e49396fab1ec29be29f19a697ff12ad0951ae3186751f2c23fc6b89b58524b97b8bb959c81be577392c7fcf6a2756ae2bdea9a47a2153a4a3b7de5eb549eea63a9eaced3dc48db6207ac8c7a2a8ec2bd3fc39a3ff62c02ff00544927d4275e4a216f297b28f4e82b1fc21a1416f6abe23d73f721941b78641c42beb8f53fd29daaeb97be27b8fb068c5adadd5bf7972fc71ea315e9d2a7f575cf35afe473ce5cef963b1d04dab68b197bb96436651f2ef27eef27df3d6b035abab7be864bc9563b8b60411776bf7947a103a8af27f1bd9b099ad74ed52ef50b8420ed8c03f89cf18acaf0eebd73a169f3ade3ddb5dc8709142032e3fda0781f850dd5ad1e68cade42b462ec7aea6a9a7c3a805d0ae43c640f3805cc9cff00b5daa5be84dc5ff906d2e19d577077bfd9bc7e55c25cd95b697f63d721d460b6beb94024b362dd0f6e076cd5f86169aeaf1f5d977f9900fb23acad8ce01c707ebd6a392f24a4ecff00afbc7d2e91d2f8a85d683a259ebba65a0b73644f9d121f33e5200ea2b024d6e2d5fc21a96a93288ef34fdb2c23b80e327f9d6068daa3c12481eea68ec2eb113dadcb6f8d49e1941e4e402319ac9d6adeef44bad62d5e40c6eed82c28a780bc6dfd2972c92717b7fc12959fa9d9f80ec5afa3b4babd449e66942594321ca4680825b1eb827f2aeef50874c7d2f55bad7ae156279362303ce57238f7e2bcefc21acc169a8417170e3ecba75b9dc3ba9c1c0fc4d5b9fc671deced3c9a33dd492333431c87112827ae01ce7eb5aa6aa25048971716d8cf12b6a3aa6976f61a0dc58e9b610366011ca19d7dc818c1ad4f0c78360ba84cbaad941aaccc007bc8e405d3f427356f41f124419bedf67a6bcab93f66b747de38e065863f5ad9d23c52ef1cf35ce9a2da20dc2db95ffc7b9acaa4a49d9b5e97ff00862a2af1ba407e1d69f6c515aca3bb8382aee332c4dd721a9f278585eda5cdbcc8ad7319cc371b7e61e993ed5837df12655d71ac6645b1847f1a92cd8f5ee2ba7b4d7a36810e9d6f77a9ef1913c7b003f5048acaac6a3693fcff0021c1a48cbd53c2b671ea9a65fc96fe5f9aa619caf71838fd692e7c271a43732cb0ee92d98347263975e7e5cfafbd7596934da85b98ef34d9614520af9854907f035a9e5acd11571d46181ad238793d61a3ec2752dbea709a41b85b80b6fab5c42929fdd9b8632afd3b62bbd856448556670ee072c06335cdc1a4476dabdc58b26609d7ce46fee3e71c7e55bd6693c5198ae0efd9f75ffbc2bd0c339f2253dffadcc2a5afee9668a28aeb320a28a2801aebbc63715fa1e6b9dd5ef1a3ba365a63c925e30cb396cac43d4ff856dea175f65b5257995fe58c0eec7a56326912431adaaee32dd1df713f703b81f9d71e263ceb952bbfc8da9bb6ad9c56bd6ba85cda7da92ea596389820979def2138f94f61c8ab577a26a57d1d9e977d249777322067925c948463f9f3d6bd0ffb3adf6429e5af970f2ab8e33560a2062c40048e4d4d3a138ab5c72a916f63cf0785a3d3e25b0f0ea9fb5b03f68be6182a3be1bfa55087c38b6933597866013dd4b9fb46a928ce1bbfcddf9ed5e8b7b750da59bbc4a9211d510f2477ae7bfe120b5688c1a598eda58d8bb5b4df2f99ebc8a249c2f777f212699c2ea7e1abf8894d01e5bcbd5e66b927644a7b904f1fad73fa8ebfabe9909b7bebfb3be51c3c47139cf7e41aed6fb5fb9d7acd85abcd6b65b70f0431aab16cfa9edf8d7013782d8caf3cb77636a9c91e6c8de61cfa8504565254a6bdfd5971e64ee86c5011a7aea5a7d90b49d816c47fbe45f72a31b7ea6b4fc3fe213ac78934d5d561ccf6dfbe6973f7b6e3f9d613c3169971c6bf6b193c0505f9fc31cd5fb5bb8ec6eae751d48c4235b29152784101d8e3b6060f1584631b36b7f99ab6d6ec658da5c68b3c9e29d5116512cecf636ea7711db27d075aafa86bcbaeeb89a85f4b7566ebd677cccbff0001518c0fc6acdcebe6f2fcc96ff25a2a08e243b48dbd7a1f7cd2c7aa588f2e3364034876899c7caa7f0a49f56b5feb40b5fd0dc86e1b52bcd38dbea3684abe05e88761000270467a5771a79b882fe2bebbbe866f3e4f25e1823da1d738dc3939c570b06a1a8a5b25a5aac6c09c2c696eb9fcf15d34f6be246b1b749e4b448c6082c3691edf28e2a54a9c124f6d7cc528ca57674b6e89e65c59457309d3e705c065ce074c0e7fbd57962b1d2bc2e6d75b74fb1c27cadd31e194700f3585a74935a5cdab6a5705e2fbaa6389767d0f19a77c5c71ff00080310721ae6219f5e6bb68ce1c8e4b5b2396a5e29b283788742d343c51ea96b7da631e609665df17d39e7e98ad8d3fc5ba1c0a82dbc456ad683a24f202e3f1cf4fc2bc0bca8cf58d7fef9a3c98ffe79affdf22bcd866b086d0fc4f29e629ef13b9f1f3f86ece637fa56a968f6f3be67852552c8c7f89403fe735c3dbebd1697ac34d6da92473db93895241861fd47b521862230628cfd50534db5b9eb045ff7c0a9963b0f37769ebe86d1cd125668f4eb1f8a7a1ea1e1bba1abbc2678930f0ee044b9e98fcfa553f05f873c2b6174dac5f5d69eb752b9922b659d0a5be7b01ebef5e75f60b5e7f709cffb34efb2c38fbb8fa1ae273c3a4d41c95f7dbfccd566d0beb13e861afe8fd06a769ff7f97fc69c35cd29ba6a36a7e932ff008d7cea6ca138e1b8ff006cd2fd8e2ff6c7d1cd737250eefee5fe65ff006c53fe567d17fdb3a6e71f6fb6cffd7514f1a9d89e97901ffb682be71fb247fde93fefe37f8d3fc9e00f326e3fe9ab7f8d2e4a3ddfddff00043fb629ff002b3e8c5d46cdbeedd427e8e2945fda1e97311ff818af9bd6d555b2b2ce0ffd766ff1a708303026b81ce7fd737f8d2e4a3fccfeeff821fdb14ff959f479bdb5ff009f88ff00efb140bdb5ce05c47ff7d8af9b8db06ce66b8e7fe9bbff008d37ec29ff003daebff021ff00c68f6747f99fddff00043fb629ff002b3e9517301e9327fdf4297ed10ffcf54ffbe857cd42d00e97175ff812ff00e34f1011d2e2ebff00021ffc68e4a5fccfeeff00821fdb14ff00959f4979f17fcf44fce944b19e8ea7f1af9b1602a722e6eb9ffa797ff1a9103a7ddb9baffc087ff1aa54a8bfb76f97fc11ace297667d21bd7fbc3f3aada8595a6a764f6b788b244e3907b7bd7cf41a51d2e6ebff00021ffc68f3271f76eee87fdbc3ff008d52a3456aaa7e0cafed7a3d8f57d2ad6d3c13aa3477a15a0b963e5df37de1fecb1fc6b23c4df109f509e4b0f0f4a1205e25ba1dff00ddff001af37bbb66bd87cab8b9b878f39dad331feb54d342b58d36c6d2a8c63891bd73eb5d50faaf373ce777e8c9fed4a36b23acb5f2db2b0390841792490f5f527fa9aec7c1de13fed043ae6a16ef25940775b5be3999871b88f4ebfcebc9bfb2210aca2498061861e6373fad69dbde5fdac49141a8dd246830aa25381fad7752c4e1212e694aff0022259a536ac8f7eb8d2b50d657ced6d9adece3f9d2cadce5891fde61d7e98accd5edb5358a1b4d134e36f64ca4caa8761fa9f5fa578e8d6f57e73a9dc9ff00b6869f69e38d7341d6f4d9d7509a68a5b811c91487218107d6bb2389c3e225c907abf2269e3212928c4f4eb6d220974a92cace01a7ab8fdfc88c21931fde39fc6b174ad3859433693a32c0b1ca486d42ee1c990e738ce46eaeaf58d662bed49ed23f2e611b80f144087719e79ffebd65f8a34ad4753b5363a78bf8a165e1a38a20a38f5273ed9a8746a464f976f3febf03d1538b5a9c178bad345d3aec41672cbaa5fc1f348aa73b7e879da38e95cc47e29323436565124059cf9f2070b9f663deba5b2f07ebfa65eae74d90db0199e477059fd79ce6b1b5cb7863bc49adf469615dd822dd376e3ea7755423ecfdd7af9ffc01bf7b5bd86599b3b986ead44a913dc021cc2c151cf6c0faf7ab1a816d7f409e68e092df51d2adbcb2cad959154801beb81535f78665bfb78fed330b69123f31612a125dbedb78eddeae78764b7bdb3bad1b4d8ae23bdb88bcb7ba9403b93b9c7238c0159c9a8ae6dedf8169dd9cfe8e67bad5ce9da56db941b8cb2939567033b89ec070455e6f1241e18b977d43fe2737ea0848add310a1f52790c7dea6d316785e4d3345d0dc58a1cb4c5c2bcdea49ce7f0ab970971e6291e1d96d9178f330acc7f0c906939456f6b3e97d7e7fe43f79bd0cff000f6b3e27f146ba6e7ed5670c4e7e6b42e2327f027ad76ba2c2f61ac491dbe8d716e778fb5a90678dc1ee30062b99ba7d3adacf173613296618b8917cb087d729cd765a56b9e1cd0ed6ceea0d52e1e6451e6beedcb267aa904d10a7ed5b72568932972d927766feabe0fd3a248b55d2eca342ae3ce876fcac0f04e3db39a90e8f1689ac41f62de2c6f864aa9e50fb1f5e78ad0d1bc4afe24b491b4ab48e387f8cdd12bb87a8c56a3588b9d3a0866b88566b7944a851b2063a0fa575470ee3a27733756fa339247d62cf57d42c16fe60f1813c44b1da53a74f6c1abd63aeea76f79143aaead6fe5cca1a298db15561f52d5b57da43dcf882dafa2da4792d13b0f4c1ff001ae7fca31fd9a396dd6e11e792d8a38e09dd85c7a743d2949b84bcae1a34771be03b25678cb6301f3d6a6ce7a57077965a3c304043ddc11dc1c6f490b053f89e3a574fe1eb64b4d3fca86f9ef62072af20c11ed5d54ea7368cc64adb1ab451456e485145140119811ae04cd966518504f0bf4a928a6c8e123666380072696c01233246591379ecb9c66b92f11f89aeecad9a39b46bc20f4fb3e64ddf90e2b99f14f8aedec964922d3753b8607064f30a27e8d5c8da78f2ff0079b9b2b9ba10ab6258a500a81e80939cd73caaf58bfc8d145ecd1b7737b2ddc464d12e26d3ae891f7ed9e3dbea59ce0550d4758b769a0b1d5215d535463e60b844da802f720e7775eb9ab4cd79e2bd634cb39fcc822b81e70887cb94ff00688e7b1e2adde6931c3a96a1a9a2664551616180319c63ff0065ae396fae9e5fd5cd52b239b9354d56f343bad535db861a740be5db5ac5f2ac8dd01c73c723f2ae5b468ef2db4f9e6b6511cd7527cd3e7fd58e72abeffcb15e87e25834d5d0e2b09a6564b2f955233f7e43d7f019cd7356b1ad9d99bd953290612d223d24931c923d323f5ae552e54dca3bf4fc8e8b295927a13269f0e956725c8b75bbd49537af9a7222cf00b7be7b567699a9b6a97f32eaede744aaa92216c82a7fd60fc4e296db5a0ba35eadd645c4910c13d5ceecff00f5aaa59e892d84716a774c564be19683fbb1ff007bf5a85749a96fd0b71d53447a9f8411af6ead638218ee2221edd80d9be3e0e4e7ef73918ad8f0ee877baa594b6b2a7933a8fdda85cc791fecfa9f5cd58bf31f88746fb6dbbbfda74d7da593ab291ffd7acdf0d6a7a969be2fb42f34b6f18cb2190021c71d6ba28c9ce9da5badee635128bbad99befa86ada66a90dadf6b1716d0c8b889972e8e40e9c74e878ae834cd42faed1275bcbd68c1e1a3b77e7fc6a7bb86d64f14688cc239e128e402a08270c4d65a805dee5aea782cfed9222c5039040ddd874a74f95c1392dc86ecec99e81a3c53c903473a23c7272ecf098f9f707afd6b0fe27afd9fc153dac6ae116e622bbba0193c0ad3f08dfda5cc6d1c7adb5da1e162b8501d79aa5f171557c0e546e1e5dc44077ce4d69ecfdc9746aff71cf55fbafd0f14a28a2be50f930a28a2800a28a2800a28a2800a28a4242fde207d680168aab3ea5696dfeb66507d3ae698ba834b8fb3d95ccdb8e06c4ef5d31c2579aba8b368d0ab257512ed159b7ba9dc59ea0b66da65d19986766d191f5e6b66cbc35ac6a6ab24b796f628c32a8bf349f88231fad75472bc4c9ed6f99b470559eeac41456ac9e061e628fedeb89e5fee242a173e99a745f0c62bbccf757f7663ddcb82576fd4035d5fd8d3fe7fc0d965f2fe63228aeb53e18e8aa57cd9eed7a6d6f35b649f8e722acc9f0a34b9cfcff6cb48f3c48b2b30c7e268fec69ff3fe03fecf7fcc713457a28f837a34d64eba3ead72f3af21d8923e9c9ae23c45e1dd4fc2c81aed4dca29c49b47cc3e83d3159d4ca2ac6378bb912c04d2bc5dca345436d7315dc22585b729fd2a6af1e51717667034d3b30a28a29082b37545df7da4af5cdea71f81ad2accd581379a561b6ffa6afcde9c1aefcbbfde6275613f8c8fa82eed2d74eb5b93a7ac1a7ae0b4f218f1bfe9ef5c46a7a9e886da39756d56e269003b63881298cf1f8d6cf882ede0b474d2609ae66320696698e6204738209e3f015ca69962be23d6b1a8dd58ba420c8f1590c800763c015f4b56a5aefa23e8e314f43221692f350fb46957b259590f9a499d0a600ebf31e3a5695d78ea7b8d325d3f4cb89d2dadcf966fae0ee92563ce17a607e75078e4a5cf853ceb584a5ab5ca45676e9c7999603271d41c8a2ffc2175631e9d6e96ed88adfcf795bfd5a39e818fb026b8bdbba8ad6b1bfb3e5dd98f169b2db492240647d4ee9b6cd36ecb107f87e9cf5e87352dc6ab0783a36d3ec9d9f52f2cbcf3039d9fec0f7e7f4ad6b6dbe14d067d667ccb279452d7cce59df1f7b9fa8ae3a28d356d2d5af76fda966ff4820fccde665c73edd2b9e528d46df4378c5ad086292dbc54278e055d3f5fb7c964fba93fb8f7fc6b63c3de1cf10340b7d1cf752a447132457014c7ea36609fc6abdee9296fa45aead140df6c96e1dc6383b0800e7e9c9aee747d5deda4b1d450659902b94fbb3a01cf1d370e3f5ae8f6b1a94db8afebfc8cbd9ca12b0fd4f48d4a4f0cf9ba6ca3528197f7905eaf99229fd33f9570b61a5e92fa8438d1512e9ced26321551fdd319e6bd8eec9b1806b3a3ba3dbc8333419e08f51e86b9c3aef867c437ec60d3e7b6d4146372201cfaf07afbd73c60d49c5e8fad8b7256b9c9cc9e28875036b6daa40d68833f678d76123dd739615bda2f896ce0b944d42d34ff0038101d7c8309c7d49ae8357f0b5bbf8a2d2e5048ab7a181dbc15217239edcd64ead603c8bab6bd8834f6832268e056665edc1eb5b4273a0ac9fe1d899463377b1e93a2ddda5de9ead64d1841ff002ce390384f6c8aadaa69b1aac1347f22c57493b63b63249ae2f43be8aded6392ef5992d61382196d9151ff002e6bb583c45a2bc2112fa391318248273fa57a31946a417358e6717196867db69be659dfc0abff001ef71ba123a9f941feb5bba75c7daec6391d70e3861e8c3ad4f1ac67f79181871d477a852dda0bd2f17faa907cca3b1f5aa8c392575b325bbab16a8a28ae82028a28a0046608b93d2b0b546d6af2530e9862821231e64884b7d473c5696a3a9dbe9d1069c924f0aa064b1f4ae7359d735386c44af8b3f30fcb180198afa9cf4ed5cd565bfbda2f2358af238ed5741d417509e0bad5966d91979b23e65183819e9d45675b7850c9369565870d7d2ef62dc9440704fe3915d9dde8b711e8d656ac0b5dea3386b876e485041233f4ae935286d748817515877cb6b01893d978ff000159c68c9bbcb5fb839925a1cafd85b4dd5f56d64100c2860b58fa7007503f1ae51f5dd32f74f16b358de493a319594cc1511cf258823d4d51d7bc49aeebf72f6eb2430c4cdf2345d3039ceec66b175f5d53c33636b1241e6497aa5c96e598f6273d8e7a74ae6509464ddf566bcc9ab5865ac96b79ae7d8e01bd230649a43c2aa8e4f5e838e07ad5ab9bbfed5d5749bc98247a67da3ca817b1553824fe42a6d034287fb25ac86efb6dfdbbcb293f78280481f98a996c97fe10381eda0f31f4c246c23ef3b738fd2945293f68fa7fc332a4ecb97b995e21fb309adace385cdccf129e46151439258fe00d375dd7d65682d6caccf9712163313cba2e381f5a96de26d7bc417e2f679a192dedd5646b788380d9ce0e7181823a567dea09e61a69d827891da3743c3838e7e82b0f64959bd6dfa9b7b46eeb634bc17716b2eacfa6c4e63892131dc37f0b336718fcc55cf0fe9f7a2feeb4abdbdb610db4a5638ae233f300718273c7f5ae57c2370f02a6970c6cd79737425694afdd0b8c8cff00c06bd1b5bb6866f10c571c7d9351023925e9b1d78273ebcd4f32a559dd5d7f909c5ce09dceb6cfc3e2ec5b48cd0bb5a86311b6994842548e9cfad639b11a549a758de2798f3dc48ee09cf2c46d07f5aa960ba2f87f505b5d4ee2f34fb8dc36491c8c5265fef727f0aeb5ec6da4d4d75692ee39edc0063c1e723a1ad25cb0f7a3f0bd77314dbdf7395b58d60d55ee174c8eea1b67f9e36e1d47f797f3adbf897730ea1f0d56e6ddc98649e16507a8e6b2e78efdf59b496df08b741982f42719e3e9819a93c717c97bf0b770511ba5dc48ea063241eb815494b95a7bdbfafbba98d67eebf99e51451457cc9f2214514500145145001505d5ec1651efb89028ec3b9fa0ef514d712cb71f65b219901fde4a7eec43dfdeba0d0bc1915d6a31970d7b70c7259c74f5c2f402bd8c2e593aab9aa68bf13be860e535cd3d11cc452eb1aab27f6659f91131e65b818e3d40e0d6ed9781cdee3fb52e6e2e771c8881c283f4af58b2f08a431450c9166e1b247f7635f5f7ed4c5d02499bcab3381236d0e3b8eff87bf7afa0a386a5455a113d5a7469d35eea3cf6dbc3f676cc62b4821b58a13fbdb971f2463b81fde63ed5a5fd9daa6a3b21d2849a4e9a47cd3b71713afa9fee83db22ba8b6d3ace6d6ee30be6e9ba461562c645c4e46493eb83b862ba5b0d10ea2ad3ddb100b646dfe2ff00eb7b5741a9e6dfd8d65a78f26c50a8e32c5773337f78fbd5ab7d32fee04715adb48c921c6e6182c7dfdbdebd62df45b1b51f25ba31eb965c9aa7e22d620f0de8725e3a46ae8308a0014018b63e0b5b4b453753c5f6973b983fdd51ec3d7deb5e2d36ce4841b59cac806649dbef63ebdabcaae7c55a9ea6a6592e42236557e6c02adcb60f5e38abda96a92d9787eced92e5fceba43336d72491c8c67e833401e81fdb5e1bd1ee0c693c6d3b1f9997e62df522927f156806e1597f7d38fbb843fcebc7ad6c6695598cbb158855900c81bba64f515d8783ec7e67b8658e78d8e14a7cc430ebc1f6a00ef935c5462d73108108caae7716f7e3a565f89b4b5f126969736a837203bb27a28e6b2756d67c849bc98e362a0ed8263b64fa715d0f85a58e7b278d6491948f9d1d40c123d6803c27c53a67f675b8d534c833b79b945e8c0f7fa8e6b36d6ea2bcb759a16cab0fc457b078a7438e19ae6d2d2dc79522ef6c9ce3f0af2bbef0adc5abb5ee90419dcfef6d07dd6edc7a1af2b1d8055d73c3e2fcce2c4e19555cd1dc868a8a19fcc6747568e58ced78dc60a9a96be5e709424e325aa3c5945c5f2c82b2f5704de6941480df6c5c13d3a1ad4acdd5066ff480718fb6a7f5aebcbffde626f85fe344f69f184964adb3537bcd49a36c948db6411e7b7239fa66aee9977a7d8f82ef9f4fb2164176a955ff006b07fad59f1146baef8d2c342b78d160809b8b9da300f700fd706abdcd811a75fda29f2c5d5daa06c642e067a7e15ece25f3bbbdb5fcd23eaa9e8ac64ebf6d732ea3a4e8da4a46e34f844d29958054c72ac477e54d4577e28bed4ef21d32e274bb9cb6d44886c463fde20f61d2975e68ed2eee56cde696eaf9b6cd70e305631d001e8726a35834fd16c2fb5ed985b48045029e59dc804b7e0735839369a8bd19aa8b4d3660fc40d4bfb46e9a0be745b7d3a30559380645c96007b802b92b55be4951920612dd426744907550401fa1ab904075ed36fa0b82d2cf1c5f68423f8f1927f4157343d75b59b75d40db8f32cac24b711e3b86503f9526dc63dec6d14ae6c2ea56f2eac34b59b7c76d108a597b21e777e60e2ace8ba7b5adbb58433c8f1a6a292141cfee9831e3d86466b93b2823b6b35d271247a94b279f7eee3918e57f0c8aedb45d65f1677de52aa5bc9f669dc28fb8dc96f7e83f3ac97eeef6d9fe3ff0e54af3432fb4b8e0d5ee625d5e6b6d3e46f2dc6ec2337520fa76ad6781347b8b64f0e4ecaf3c1f2cca77aab2e061b1dbf2a8f50b596e75597438218ee6168bed93311ddb2179faad49e16d534f86f238ee6d25b5742607661988e38cfe9e95b49cd38b8b76d3eeff008073a49a69ee6ff843c4fa9dedd496be2636704d07dd390a5be9935d44fa740f24d749862f195e0f0735e77e2ad29eef447d4e3815ef6ca528bb495f35300f247d4d5ff0478c1e4d3121fec8bb917f89a2632e0fbee3c575d392ac9c67bf7f55fe46528f27bd116c74b3a55b699731a9d8dba298377524e3f5354747d5fc45a7f8a2e6c674b56b53314491d395c9f94139e322bd0dedadf56b25431bc2a8e0ed23041073547fb162b8b9d4bcc553e714c63a8c0349c6ad192b2bafe9ff0098f9a124ee6b59fdacc00de18f79e7083a0ab35434a91bece6de524c909da49fe21eb57ebd2a6d38f347a9cd2ba766145145684851451400c30a1937b2866ec4f6ae6e3b07d77c4725edda37d8ed4ed811863730ea71e9906ba7a318e953caad61dd914c224512cbb07979dacc7016bc77c69a93eabaf4b1c9a9ee8106120b604313f5e40faf4aea7e20f8974ab4b4fecfbc9e496e64385b6b762092781923b66b97f0ef832193c3d7ecb216bc950bc9f3166419e101ea38ebf4ae3a95ad249e97368c34ba29689a18b0d022d6ee23f2a02e64f2e439698f6fc33da9ba83bebf3f99aa4c62966fbb102372c3fd074ab3aceb10c3a3c666467b6b4fddd9db0ff968ff00de3ea39fd2b8fb6b1d56fef2ecef67d42f63da801fb911fbd8fa1c579ea73a8db8bb1d5cb182d4b5a56b77d0eb0751b1b4375f68f312340c30836118feb56346baf216f92f27611a325d4d6e3eef424e7f3ad6f0ae96da3e856d75b4486d2411b8c75663839fc0d62eb3a50b49b5730ceaefa94ff215e76c7cee1fa8aa9c9c97b35e9f97e8104a2f99967c25a85a596aced6886e6e6f60ccb1e307ccdc78cffba054ba9e856f79e20b7d4afc2c7242706da3b772ae3b8661c76eb54f4b827d465bb4d2e586ddd889e1c200d9c05fbdd71c74ab37bafeb56b1c16b77094d4b3b1554e527fa0e950e50855e682d5ad75d83966e3696c645bcda668f75ad5fddc72437664d9023ae4608032a40e7ad75505c3dd786a1d3a1578d628926fb430c8126395fceb2756d22e6f34956f12c53b5d201e42c10058d0e73cb0209fcab5bc4be7e8ba3787ad2c897768cde4fb4677fdd247eb5b7b28b7ed13bfcffaf333e7925cb6b1a5acc1ab49a7da3de59db4f7318ff459e31b9271824ab007238cfa74a6784bc5fa3dc5c2daea3a32d95d2ff1c192bbbd31ce3f1abfa5a476fe1db6bdb5bd779addc096dc9ddcb1c700f4e1ab0eff00c3fa4dbf8c6e6496f66d34cb2078c1f953764e7773c67b54c7de9ba6d6ff009ffc12744b9bb1dfa597f69f8bad2ee06dd650c07049e7792463f23583f1274afeccf06dcec3b966b9888ff64027fc6bb5d2a09a1b184ba42f85c6613d7dfa5617c52f2e5f02cb9c8db32601eb9cd38a6e128b5692bdbcd7f5e6635adc8df91e13451457cc9f22145145001505cb48cd15b5b7faf9db6ae3b0ee7f01cd4f52787628ee3c4524f745c222b470151fc607cdfa115e865f4156ad696cb53ab0b4954a9aec8ebbc27e0d5ba9e3b7b48330ab032c8c3ab7726bd874bd0ecb498152da150c060be39354fc21a79b0d022debb6493e66f7f4fd2b741cf622bec0f7c428a720a8c11cd67eaf75168fa25d5d850a6189b601dce381f9d5abcb84b2b77b8933851d33d6b03c530beb02c7494629e74ab34a57f855086c7e38c50047e10f0f8b3d1eda798e4ce5ae9908fe390eee7e9922baa0028c01814d8a358a354418551803d053a8020bebc4b0b09aea504a42a58815f3f78dbc5f3ebdaf490c3b8421b11f04ed039e00af4af8a3ae358e902ce09195e6387c7f76bc634f97ed173841962cd83d18fcbebdbf0a00ea34cd1234893ce88bb1c332971f387e785eb4dd527b69b5225622d1a81e4c29d9738e3df39ae9748881d31665dad2c308e5c6194e3b9ea6b274dd1e28ee7ef125beec9d59b9cfca2802ef86ac92f2fa28678d92272c04d1f0d9cf19278e2bb0bdd3a0f0fda4891299219170cd311f5ce78aced3ff00d03e78432aaff122ef3f8a9e14fd2ba5b3b7b3d52d434e924c7cb208762ca7f03de803cb2fa717b24cec5bca8c81186e190ff091ea0fb5779e00d42492dcc0420b71f2a008410dd7073edcd727e2a83ec972b6f0db88a13fc6dceef607a8c7a0ae97c1e4c21011b028c4849ce49e87f502803acd5b494d42da5f2c88e7642ab2579bea9a2c9a3cdf679915a594e637af48d2b515bcf3e127325b4851eabeb7a0c7abdd5a4c4ed781b24fa8c1e3f33401e1be34d285b4035985556e6dd717033c48bdff00115891b8923575e8c0115e97af68915cdc6a168d991378420fa36735e4ba34ae609ade55daf6d2b4679ed938fd2bc3cda82705556e8f371f4d38a9a346b275a90c575a5ba8c91789815ad593ad7fc7d695ff005fa95e3e0bfde2270e17f8d13e9d8e3b6d012f359d407fa45c3718196dbfc2a3f335c6cba9dcea334d3b442d6d2de4de8ac7e6918f61ea79aecbc5fa8c3616b09fb3fdaae59b10438ce58f7c7f5ae32cf4c9aebc41a29bc75699a5798c487e4451b863dce4f5af6ab2e69aa4b647d545d9733393bbf134d71aadcda5b5b4973abcd72b6a5d90ec4c37073dfef7ad74cda0c6a25f0fcd30b8b98ed8cd7193d6438238f400e2ab2cf1db4d359e9d1a1d523d4a476774188c7cb924f7e956b4647bcf1269faf473099ef4c9693b63a95623ff0065a8a94a3ecb962b57afcec691a8f9eece434cd1db4c8f4ac026e6e166b6954762531fa66ab5bdcd85a5de9f6da65be0437845ef96858617702588e0735a5e2d44b3f1aea3716773756e2291bcb9026f85253c32904e076ed51f85741d3f59d3e7b9b5d46449c391769136d5249249383c83d79ace7054d73d57a797cf7fbcd633727686fe7fa126bd721348179a5c2b717dad4c23531f3b63046578f626b6fc37a4c775a35ee88b8f2d2dd85c49dbcc382aa0fb0c8fc2b93d174e92e3c4e9fd8733269b6538f364918843cfcc54f6e3b0af45bfbcb5b3d423b2d0e10b6714a259e407efbb73c9f4e4d0a928c77bdbfa57f45f884aa36cc3f0cadc41e1cb8b8b8778ee6f654b305dc1609bc679fa135d74fa1ae88d1bc8e6eec0011cd16413183d1c63e9cfd6b3f4fd1e4b2f10037d0f9fa75c334f116e3ca6c7231f870696c75b934dd1eeafefb2ed757cd1430bf3ba356603afb62b4a72f691f7beef2ff806735695e26a78d641a4f83edcd82171e6a0551d581619fd2b8031b786bc450dec7a8de41677ec5996060551b3c8c633debd6e0feced6f4a464424400e237ea8d8e845793456f772d991aa5beeb79351920898310572e718fcab4951e595d76ff244467ccaccf5af0e6a56b7b660c17d25cb1e7f7dc37e5815b21406240e4f535c4f8312f2290db4ef04925bf0ea6155641db91c9aedebd3a52728ab9cb2493d0688d0485c280c4609a751456a925b12145145300a28a2800accd61b51f2b6e9ef0c2b8f9e69ba28fceb4eb23c47a7ddeaba7fd8ad1c44929c4af9c10be82a27b0d6e7972dce9afabddca8a6e85b0324977371e73ff081ec0815a5e189ee636bb48d4869e1697a64e491fe359fa8e80ffe9d611a85c5f08fe5e76a6571fae6bbf8b45934a897ec2a249da1589588fbbc0c9fcc578b387336f63b14ac8e075ad2df4bb416b67fe95a84a32a71c4118e4fd38cd53f0c5acf63e27b6bc9be7b6fb3bc6642781bc83fd2bbcd7ac5345f0f9318f32faea448cbb7de70586e19fa135ce5adb345a2eb964509440248413d319c8fd69aa4e97bef71fb4e65ca417b65a8daaeaba7696214b5917cd69e4c92a4f000e7d85727f618a6f0cdec90334b73a74d0a6f7e4862adbbf322badbfd46f6e2ce36ba48ec61950c82dd3e669368c82c78c0c8acaf876f1ebba86a56d344224bb52d2a0ef22f00fea6aa945c55f7697e2294afb98a7c28348ba96c56fcc77de5f9d6cc7eeba91d07a739eb5369b1aa44adac4b6b7b783866bc243c67fd920818f7ab7aedcc571e28b364f362b3b5805bc93dc2ed5760c4e01fa1aa9e2ab2d3fc43756cba444f222031ddcd147b86de3bfa8ab94fdfb4574ded72145f2de4fe451d6751d6350d50e996f3adc411a7121601507bb7427e95d9c91ca9e09b7d4f5268e6921896da0d9dd40c13cfd0571b75a8f87fc3f6d146aff68320c7cff74fbede95e89e1fb61ab7c3bb4bd92359fecf33cab1a7dd65073b7e9554d41c1a8ab2fbbc826df326ce674147b936504f32c53c5261cb64066e587e98ab3e25b7975dbdbdbbf308dce2df38e048b90a41e951689024bae96d5f090ea25a4b68f77ddc02a0fe6b8aeaefec2eb4cf05cba54c91f9b72ffb9931c867feb5cf522e9ebd1bd0a4d48cbf87fa9ea473a726a7024e8d8315c2b6f61ed5d27c4e671e0397cf5f9fcd4ce3a579f2c371a478a63d4aead4cb0b01e6b21c3c4c06383f419fc6bb8f8817d05ffc3633d9ca6581e48cab1ebf8d6d1574e4baaf95ce6abf03478a514515f2c7c9851451400c95c470bb9214282724d68f8024b9d5edacada184ee33bced2943c9603807f0ac1b8b79f5bbbfecdb50044180b87638c0eb81ef5efdf0e3c396da5e8a93247cfdd8ce7f87e9dabeab2bc3ba54dce5bc8f6f0549c21ccfa9d3e8b0dd41a64697d8128c8c0ec3b55fa0714c761b5d4361b6e7e95eb1dc707e3cd7c8bb8ec216c45110f3303dfb0fd6bacd2ecb0c2fa562d34b12ae0f4503d3f3af1bf1a48f6ba9bc32fc926f2d92d90f9391ff00eaaf5df085e9bff0ad94cd2798de580cc7ae7de8036a8a4072bdc7d6a0b5ba4b932ece91b9507d7de803cbfe300905dd9ac60832c6dce338c102bccf42b722f511065816de40c9e9d2be82f1a69d0df687286456b82a522e32d93d87a1af15b0b79348d57cb6f94cc0918e49c8c7ca3fad007a62db4b6be138136bc71cc8a5a37c30db8f6ededd6b2da13f218e1768fb9eeff5f6f61cd761e198a1d4fc2f14732b10bf273216208e339ec6b4ae34789a08e2b7022541d87f2f43ef401c5db00803dc862e305003f73fdd3dbe879ad7b1d4134fdf7d7112c719500484e1df9c608ffeb55a97467843f97192dddb6e42fb81fc47deb9bd66d25bcb61932cb1ac8388c6e00f4e5faafd28027f1969cb75341aa22a3425416dcdb5587a107d7daa1f0d5e456f25bc00ef24fcaaa8c14a7d4fbd75f3e951cde1e86da68e36d91852653b828c75e7bd61b796acbe50540182c400dbb4ff0077db239fc6802f69929b3f13cf090aa974ccf9f5f4fe75d3d727e228ded16cf5658f698b1e62e7a7a0ae9ed6e12e6d639a339575072280389f1124365ac5dc842a45e509a57638031924d785da38b8bebfbc8936c3713968fdc0e33fa57b5fc6e1e57c37ba9631b59e68d2465e094c9c8cfa578d5b2aadac623002ed1803e95e366d539692877fd0f3f1d3b5351ee4b593ad1c5d697c67fd352b5ab2b5ac0b9d309ed7895e2607fde2079d86fe344fa77c54d6d6762f78e14dc2a32c658fddc8ae4b55baff00847752d16ee35ded2db98e30075763b8ff005abbf1473359436e8c773488460fa1a5d6ad9aefc39a15d5a44935e5bca3ca463d4ed20ff3af6a9fbd52a493b599f572568c5773817bfd563bdd6f56b705edec94c724857fd6bb6431fcb15d978234d31692ad8c793711dd4791d99093fab5674ca896b7fe1a80f98b6d6a64ba93fbf290739fc857a468961143a75a10bc1b58d707a1f9455518cbda7f5fd741ce4b94f0dd6fc5f71e1ff166b108b637625be655465f94b1c0ea78f4ab16361a859d94f7b75a1dbcb25f65a505fe655edb70de95a7e2ad3359b8f146af2787e2b29ad9252658e68c3b093b9008e3b571979a878874ebdb5b69b7a1972213b8900f7e7fce289251a8fd8b5cdd6e54758fbe9d8b536bff00d8f6b1e9cc8cb637902b5a861f3a124e4363b8c75aed1cac9a6dadae9ee93db430ab5c4eadc1c807667d7fc2b163d2eeadf426fb3d9423f70d8bbd446f91ce0e42a303b7f035d2699e1d8e2f0f5868b68a52dcc42e6fe61c60b0dd8cfaf3c536a95a4e1bdf5ec4de774a45cbdf12deae9d1492a472da4c815d507cc8adf2ee1fd7e951dfe9526b498b42b2dbe976b98c8fe37600a9fae01aad1dbc5a8c72a2bf966691858a83c346a33faf22bbcf09e969a76881067f7bf33291f74fa56587839d4b3d2ebf02a72515742f8508bcf0cd95d1e1e68bf78718ddc914cd43c3e2686ca08946c86f05c31f6c9ff1ad9b2b44b1b34b68788e318518c62a7af5bd945a39399dcf36d274bbafedf37b6f3982e1862366fbb20c9f948af45b7331817ed0a164fe2dbd2abc3a5c10a81b776c72e87fba4d5ca2946515ef049a7b0514515b121451450014514500141fba71d71451401936ba5249109675f9e497cd7c8e73dbf956b60673de8a2b38d38c514e4d9cf6bb0fdafc45a3dbb64a0777618e385c8fd4567eaf6506973dddc344645923654857ac8cdcd7486c99f58176c7e448f6a8f7e73597e28865483cfb305ef24fdd423b2e7bfd78ae7ad494bde9773484ada23ca62bb96cfc5b35aea576b712dcdbb068f1c42d82428fd3f3aee3c01a0c5168b677bb36cd1cb20703ab7cdc66b9bb9f0b59e9da8e9d7131796717c565919b976c0ebf9d7a869b689a75ccf0c6498e63e64631c0f5fe7492537ebfd206ec78f6adaae9d7fe3e95089ac2d668b64d1e010d264e73c1c718ac8bad3f49d0ae6ea2cc8d6f2867b4646fbcdfdd3fd2aeeb116993f8bf5ed3351324371f686961746c73b00c7e954b44d06678e29b519ae9e546060864b7fdcf1f777367e5fcab92a272a8d26d5be5f73358da314d925df841ac7cb9aeae203712a9689b1f30f973df8af4cd3a329f0882e9ce1e69ed8e587791873d3debcd74fb5bbd58de5fea51cf7125a168a384b9451c67713f8f4af5df87f6f12f826c808d4061bca81c026bab0ca6e2d4e49bfc8caa357d11c3ff0064dbcde288fcd3fe8ba3dbac527d59b3fcdeba3bab2b9b5d7ad45cbf9ba4c05a45924e401c6071e95b377e19b58f48d59416dd7ee2591bb8200c7fe822a8da2bea9e16b5d31cb14918c6ed9e4a2e01fcf353569a70bcbd422ecce4f5c7bfd2bc3526b16501905cbe406c1da43e33f881fad5af136a106a9f0a85dc512c13bcb189a21d9bbf1dabb6f116951cbe0f9ac624e1117601db041af3cf16592c9e0ffed1b43b0348b15da6782ebd180fa93cd66e9fb38dadbabfa1153de8b3cd68a28af947b9f26154b56bcfb15833a8264721100ebb8f02aed73da8ceb3f8960864936476aa24c671b89ed9edd3af6aeec050f6d5d27b2d59d386a7ed2aa5d0efbe1bf85d5afa08f0d1bccc1a691b92c7ae2bdfa6bab5d36d879d22468a3a7ff5abcc7e15586a05bedaec040ca790dbf3cf407b5696b33dbdbeaba84fae5cb2dbf9db61403737072703f1afb13e80e920f16595e5bdd9b62c6484121587defa579dbf8c6e1b527fb579a89fea8480f2c3be3f3aeb7c28de1bd42eae1b4695fcf64c3453f0c3df15c7f8bfc317c27916cd7e68c92cbdc7a37f9f4a00a3e24d3fcb9be60d20910491367a823eefd79aee7e156a2971a0bda6089206e4f635cae8fa55febfe06b98e6497cfb27fddca41cb8e7207e356fe1bca348d64da4c582cd9c0ddf2a9c7eb401ea7a8dcad9e9f35cb0c88d09e2b98b3d54d85cb46b97318113a9ec49e1bf1ce3f0adbd70bcb64d6712826e14aa963dfd2bcfa7be9edaee45b818b94728ea4e0918fe7401e937967fda36491c840271bf1dfd4579ef8dbc376f6a61bf4cbdcc38c8c7ca066b5f45d6ee2043bd9a64543e564fde3e9f515d5af91a942a648924438dc0f3cf5c50060fc3f329d15fed28eb2ef2ff00363eeb648c0fa575751456f14323bc4a14b800e3db8152fa628001cf515ceeb3e1d9752120668e48b21955b208c1ed8ae8a8271d68028b59197488ed93f778500a9e8463a1ae726b292def6381577b6e0013fc43aeefaf6fc2ba89ef560be8207c01303839efe94ab6312dc99c82d2672093d3e940143c4903c9a0c8113cc31e1c8f5c547e12ba173e1e80672f1e55fd8e4d6d328752ae01523041ef5cb7845e4b5bed434e911418dcbee539eb8e3f5a00caf8d72a45f0baff007e32ccaa9fef1ce2bc56d73f63873d7cb5cfe55dafc62f130d735fb7f0cd936fb7b3613ddb0e46e07e55fd0d71e06060702be7738a916e30ea8f2730926d442b2f59ff008f8d378cff00a6256a565eb1ff001f3a67fd7e2579b82ff7881c786fe344f7ff001bc52ea1e25d3acedbf8034b29c740307fa53ed6e7fb2fe1ec776c37dca3c890a1ee4b9c7e95b16f662e3c6fa9c921c8fb2468a33d325b355a3d1259ff00b2ed2e10ac56b33cec847def9980fe75f43ec39652d3e2fcee7d4f3dd2f2397f0b59c87c65a969b73f3cb1d846277fef392f935ea56f1f956d1463f8102fe42b89f0e593a7c4cd76e24046f442063b12d8aeeaba685351f79113937a33c3f5ff00133e89e34d46dd255225d432ddb68c8c8fcaa3d7f5a6d4b54b6b9b58668e1b66ca3451649f5278231fad5bf1678566d4bc6bab47691c53e556e0a48369cb12387ea3a55087c7173e0fb6fecbd634dd970c87ecc1463cec718e9dbd6bcdaf082aae504a52edea74d39371519688c7f1878aa59fec967712aa3cd22a828085233f7867a67a60fa57afc901bfd3b4ad2ac768867811aea543fc0aa0119f5e6bc464babb9ee1f51bad092eee141612483f750fb05c60fd6be81f06d90b6f09d96f501e6884ad818c6f1bb03db9aeea3469a8722309ce57bb3cfb5f90c1e21b6934f81a3860b95b68dbf87008240fc0d7adc000886d1852323f1ac3d7f43866d2628eda301e1955d38ce4e467f4add8015b78c118214023d38ad2952e496a4ca5743e8a28aea330a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a618c190bb60e3eefb53e823208a99454b71a76391874bfed6d36fcb4796fb679909fc179fd2ba886371651a392b208c02ddc1c5334fb6fb1d8c709c654738efcd59a8a74d417dc394aecf9ff5cf0b5dbeaba8dcb6ebabdb0bae268f8694707e6cf18e7b56e59fc42b2934f4f3615827460b34522e303b914ef116b3fd93e2fd49239b1e74e24383d06067f95737ad5858f8875a1aac33c36f6ea57f752100cc47afa0f7af1ead1957ace15168b6676426a9c2f17a8d9c5d6a665bb82eff00b36dee09699a5fe23feca8e7a01dabd9bc06a13c1d66aafbd42e15f18dc3d6bcab5fd7b4cb4b62c6d523ba78fcb0aeb92a7d54fa62bd4be1fddc777e0bb268e457c200769cede3a57a1846b95a51b1cd56f7d59d1c89e644e87f881155b4fd3a2b0b54863e76e4e7dcf5ab7457638a6eeccee23a874656190c306bcefc73a40d27c057c81f72c976aea3fba09e057a2d721f143fe446b8ff00ae8959568a706fc9913f819e07451457c29f3015cff87e06d53c65790a2bf98d318c3a95195cf1f7b8adf3c8ac7f0249e5f8e7508a455991652763c7bc673e95ef64c9734df5d0f4f2f4af267d39e18d24697e1c48632524d9cb920f3f8715e19e3e8f54bfd416c358bd5b88966091ccb94c007927a72457d01a7ab49e1f8c5a6d42d1fca02edc1fa76af3af15fc2fd4b5cb691a4bd5da83cd08137348e390339f515f447ac723ae6827c151ff006c68f3c16be48845ac3e6963724a02cc7938c1c8ec2bd0bc25e2c4d47434d5755944d34efe5794abf2a818e79e7bd79c691f0c3c51e22bd5fed2416d6b19da64938240ed8ef5d9dcf87e3f0c4896b66ccd0c18d9bfa97edc77e71cd007aa5ba442dd7c98d5118670063ad705e20b1d3f4ff1ad9cf182b23b6e755e9cf4aeb7c397b757da4472de47b1fa7d71ed581aee957979e2a5bb68c47696a8246908ce7193c7bd0067f8d35a163aa5bcb6f2323c203b0f7ec7f5aa7e27d2bccf172b23604b009c63aefe7fc2b84f899e354d4ef07d963302a8d8ce4e778c8fd78af6dfecbb6d6ac2cafd389844363fb6280388b133c934b716a32634dc14f4128e07e7cd777a0180da7d91373180ed624f53d73fad54b3f0d3dacae55c61ae1263e840073fcea6d1f4792c2f8cad2173e50439f5dc4e7f23401b9818098c0fad5792f922d463b572019172bef56b68ddbbbd73facdb496faa5b6a65f7888ec09ec7bfeb401d0d64f89ae9ac740b8b94e76153cfa6e15aa8c1d030e8464565f89ad56f3c397703bec0c9f7bd0e78a00af7e45ce9767aac2a5fc90b2853fddea6b6a1944d0a483a30cd73ba13c973e07103ab996388c38c7270319ab3e1bbd3342f6ec49f2feee7d3ffd7401b95e53e3bf17c1e0a9afa0b40efa9df2797093ea71cfe0335ead5e03f18112e3e25d92aae0c16be61c773b88cfeb59569b853725d1133972c5cbb1c758d9b5aa3bcf299ee656dd34cc7976ab74515f0f52a4aa49ce5bb3e6a527397330acdd506ebdd2c74ff4c4ad2acfd47fe3ff004acffcfe2574e07fde61ea6b86fe344faca2b28e2be9ae949df32aab0fa67fc6ace06738e681d28afb43e88c8d2ecc0d62fef46419088b07fd924ff5ad73d29a91ac79d831b8e4fd695db6a3301920671eb5108f247946ddddcf0af18dfeb7a7f88f58bdb2f32531481198e36ed439038e7b9ac4b1f12af89e7b49b5581a44d3dcced70232d83fdde07bfe95d5ea5ac5d5baeb92ddd80097cce0189bcc3112318718e2b2b4db0d5746d108d1eface083cbdef170779619f9bd7ad79751537525ed95bb3ee74c1cb957b3d4c9d43c632f882d069fa6d9f9b2b445a4112e3cbeb927f015ef7e162fff0008ae9a246dccb6d1ae7e8a2bc47c3b7da6f86b44ba693c91a86a16ced72c14284dc080abf8ff003af6bf08c825f09e9ccbd3c84039f615d785853a778c0caa3949de46c3a2c8bb5c64528e0714515d864145145001451450014514500145145001451450014514500145145001451450014514500795ea7a6bdef89352b69eced2e364bba3772c24c6074c71f9d61eb7ac58585b369babdba968c89012bb4b28ea323bfd2ad78975a6d3fc69757c6198db248c8d2142003b3a67f1acfd71ecbc736162b656fe6cd6eaad23938e31ca8f535e3568d4956e571baeeb7475439542e9ea50b449b5287fb6aeb4b8963b78fcb83cf2761e783c1c93cfd2bd87c0963f63f0bc123b2b4b71fbd72a300679c63dabcc7c5fe238cd9595a58a6d89630de98e70463f0af4df00df457de1381a170db4952339c575e0e73943de5633aca29e874b4514577188571ff147fe4469ff00ebaa57615c87c5019f035c60e3f78959d5fe1cbd1913f859e07451457c19f32154fc2fa75cc7e2fbe317dd930f951c8dc4e2ae55df0cec6d7af23965963568a3c79485c9e4fa74fad7af94cad5daee8f4300ff0078d791ef1e114922d2bcb9a59a465c7126df978ed8adfae2bc31ab5bd8ff00a3ca63557231fbd0cf9f522bb5041191c8afa93d90000e8315c6f89add22d6239ee2453091f2c43aeeeec7e9c1fc2baf9a68ede2692660aaa324935e5fe22d69f55f145bd9d82fcd7122c67d7603f37e84d007a6d9841650f9472bb060faf1d699a92c6fa6dc24cdb51a3605bd063ad4d0c4b05bc7127dd8d428fa015cc78f757fecbd064759021c7241e403c74f43d2803e79f1835bdbdacd6b69ba4904cc77f1c8c9c75af6bf0ff886fbfe15bf87eeed3059c224d9ee0b62be7cbe76bdd72e2e0a3344c8554a3ff11e7f4af68f84172b7df0bded99434965718e79da060d007a2e8bad2ea3a8de4593f236138eb8e0feb5b45727838fa77ae6bc2f098aeae4c684c723172cdc6d27d3d8f5ae9ba50047712f91017eb8ed599e225074b170588f2486dbfdecf14fd5af0022de22de6afef0f1c6d1d7f9d5cb848a6b16f3d03a6cdc548f4e6800b0904da7c0e3f8a307e9c549711f9b6ee980770c7355b4878e4d395a1042124807b55b90131305e0907140183e1b90a5e5edabc99688a8c63eb51c70be9de214258243213903f9543a5311e28008c3f96cb21e9b88c60d68eb31a24d1cb20f93962475c819fe4280366be73f88379f6bf8afa8a8c916f088f3e9d0ff005ae9359f8d77d77e743e1ad382ed62ab3ce78fae315e7b1a4d25ddc5edf4c67bcba7df2ca7b9f4fd2bc9cc7134d51953bea7162eac15371beac9a8a28af953c30aa57a9bb50d331daed0d5daa57a48bed3483822ed2bb303fef30f53a30dfc689f5c0e94503a515f687d085145236769dbd71c5007906b5a0da6b5e20d4c35c7d9ae05f32aed63f3018ea3a5725e23b3b8d23c41a7e8369725dae9b734a4f0a7dff3ad3f134b258788aef515bd22e62b93f698bc9215791920e7934b2da8d4214bbb09acaf23b839769e306404f39dd9e31fa579356128d4e69cbdde9ea75539271b456a5a6f0f4d63a6b46905a24937ee99e425a5933d49fe11c1ed5ec3a2da258e87656d1aed58e041f8ed15e0c975aa3ddc820b859edad1db6dcccfb11b8fef1e09af73f0d5d4d79e1db39ee000ed18fbbd318e2baf0bcf66e66557953b44d4a28a2bacc828a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a2a96b3a90d2347b8bf30b4c2042e517a9a00f3a9f4fb4d57c5dae4d7526d48e6f25549f949c039c7af38ae6752874cf0b5d496975be256914abc44e486c938fa5375ad46e45fcdacc76a16c6f2732930c824d8db4001b1d0e40aa7ab6ab6dacc56775aa34504f6cc921b761933edf56fe1cfa1af1a71af2ab2edf97a1d7194145772e681a43a453ead776cb0a364c46e4925476da3907f1af55f01daa5bf8651d4ee79dda466c019279e82bc8f59f1725dda416eae67258aa2c7d4a638e3eb5ea5f0c6fdaff00c1713491bc52472ba323f51835d783955926ea686559453d0ebe8a28aee310ae3fe28ffc88d3f6fdea57615c7fc51ff911a7ff00aea95956fe1cbd1913f859e09451457c21f3215268173e478a2642a4abdb156fc41a8ea9e993dd5bf8c6e24b60ac1208f2ad1960725abd6ca55f117f23bb03fc5f91edde13f0b5d7d962bb5be288dd55914923d8919aefe35f2a25466dc40c64f7af3bd0fc5fad35a46ada23ceb1a81984ed1f960d773637725ed92cd25b35b3ff0071b922bea8f6ce47c7fae08a25b580f41ba439c11ed5c2fc38864d7bc6973a83977861fddc241f4ce4fe46aa7c4dd6676ba9edd1b75cdd49e4c4aa3393f4fa0aee7e11e890e91a129588ef51b0b01f79bb9c7e3401e8c488e2cb1c051c93dabc1fe27f893ed3e2116f14bbe355208fe122bd3fc69e228b49d35e367d865539dc3803fa57cd7a95e497baabdc163279a7713dc0f534015f52b2bc8b4d17b14605a293b554f439ef5e89f02efa56bbd6f497f9527b7f3221eae739fe559d3785a7ff008562f70eb2079261304ec1403838f706b37e1f493f86bc4fa56a441482694c2fbce0b861b46077e4d007b77862f2437112337cf039825527ef64f07f0c63f1aedb219b03a8e6bcab54967d1bc4571159b344524f34f1feb37fcdfa57a2e977b1dde9f0cc8241e6206e9401cdf8d2e5f4fb9f311ca899369c761dcd741a6b3dfe80227675729b19ce327deb2bc71611de69a8cd90cffb924f401baff2abbe0fb95b8f0ec0324bc436313dc8a0087c2e4dadcddd8bc8c7cb6f903f5239c9ae908c8ae592e3c9f1c46920dad3c6d81db0318fe75d4672be9401c73391e3f8b76e460a542f6238ae835c81ee74e78d380c8db987f08da6b3ae74d9dbc610dec4b98d540395e067af35cdfc4cf88d1f87603a36931fda753ba42b8078841ee693692bb0d8f18b1411b5dc6a72a975228fa66ad5416501b7b55473973cb9f563d4d4f5f1189929d694a3b367cdd69295493414514573990567ea4c16f34ccf4fb5a568565eb1ff001f3a67fd7e257660bfde206f86fe344fafc74a281d28afb43e88283d0d14c99cc7048ea32554903d680380d3c5a5d6a7ac5b5da452caf79231565072302bcafc6fa1b689e2482cf4b9644b4bd6dc515beefa8f6e6acea1abf8834dd6a6d7e28a3115fc84a143b8425b8c381d3f1ad782d2e62856f757d3a2bd92e41dd773306201ec9e95e27b174b10ea4a6acfbf53b1494e9a825a93dad869f616623bd5fb54f2c65542b7c91e4606077fa9af60d0c2ae8566a8000b0a2e07b015f3b6a8d73a2eb22de18e7b95b801ad5532e5573c838af74f016acbab78520956392368c98dd641c820e0ff002aecc1f334e4ddee675b955a2ba1d2514515de738514514005145140051451400514514005145140051451400514514005145140053645578d95d432918208c834ea8ae92496d64485fcb765215fd0d007ced6fa53cfa5dc2c77b25bcbe69cc6dfeac82d81c7d6afd95fdd5b5c45e1bbdd3edee2e8e63173e582b83c29ce3b0aa173a78bb99a3b1d427698395bb81d48c90d9e1ba7a574905beb4f62a9a79b43664619186d24f7dc09ce6bc8a908f37efa5a743ad3baf716a64de58dce89b604b582c96760be74ab992524f6ebb7f0c57abfc3b8d21f0a88a31feaee2442c4e4b107ad7947894ead7d790e972279d25bc61d255e429cff0011edf8d7ab7c3a90bf84a35740ae92bab907219b3c9cd7561255271bcdfdc6555462ec8eaa8a28aed310ae47e270cf81ee38cfceb5d75721f13db6f81ee31de44159d5fe1cbd089fc2cf03a28a2be0cf990a93c1d776e3c49a8195a1dc4ac7fbd246304fa7d6a3a87c2e61b7d535059d63def3165678c9c03d39af6b27fe2cbd0f472ff8dfa1f46687a8e8f6f670c16d791b3301c0cf26b6d97f76c1491bbb8ed5e7be197d06cede39ae6e3cdb963c045ce2bb6bbd552db4a6bc0ac405c8571b49fcebe98f60f1ff008b9e1cb9b0d574bd5f4cf2a516e486809fde48e4e7207af5aea3c37e3cb3d3ad60d2357b39b4abdd8028b95015dbb7233595a1c371e33f8a11dcca4496761fbd6ddc90dd973f8fe95d67c58874cb8f0c8b5be811e69982c2c78319c8f9b3edd6803c8be26eb9793ea4fa60f3199cee60d8e476c7b60d735e0fd064d63535fb631b6b2b33e6dcc87a60738cfbe318aa36f7b7b75a8369d7624bebb85cc769764e0228e39cf515b52cef269e347b390fd811f75c4c0f37527aff00bbc0acaad58d1839cb644549c69c5ca4753af78efedb0ff666836e05928d86e5c761c6147f8d70b7965e5451dc879249ade459412c7036b6781d074ad25508a15460018005437a336170338cc4c33f857cd4b32ab52ac5ecafb1e3bc65494d7447ae788e292e74dd13c4318cf9b6c91b81df72839fd2bbfd1443fd936c203b9153e535cbf852e22b9f84b632ce03886d5402c73caa815a7e10d563b9b08ad016324716f6cf6e4f15f547b66b6b361fda5a64d6c0e0ba10a7d0fad62f81e216da7491794eb233967dc7a1e9fd2ba9aaf6d6df6792e1d981f35f77d06280391f19cefa66b16ba9243b840a64661dc0c715cff86fe38e9fa8b30d7ecdf4c424f972b728c01c7ae6acfc57f1969f6ba15d6956137da752b889a258e26c84cf76c74c5790416c8b649048a1801c835c18cc62c325a5ee7362310a8dba9ea1e28f8ca92c1259f846269e62369bb71848fdfdff002af318e191ae64bbbc99ae2ee63992573926a5550aa154600180052d7838acc2a57f756913cbaf8a9555cab44145145798718514514005666ae09bad300e4fdb12b4ea8dff0037ba6f19ff004b4fc2bb303fef30f537c37f1a27d723a5140e9457da1f44148d8da7774c734b515d4665b49501c164233401f3eeb1a5e9377e26d5a3babc9ed2ddaf5a389637cab74e319f7aa3a85aebd6766f61a13c9aa69968e2512bbe0a9c7dde7a8e7f4a6eb1a5c11f990fd92f16ea19990c9102f1c878fe103827d735a97fad685ff08f41f6ab075b8815636b5b818dbea4023bf5af2ab28aa976f9976d343aa17b69a157c3715b9b0ff4bbe69e7647dc8a7ee60671bba83f4af5ef85f1ac7e05b7d808066948dc727973debc4ef9f4ad47538c7867ccb631ee4b930c4645e4632157a9f7af64f84e641e0d58dde4912399d51e40413f31cf07a56b85726eeefe8c9abcb6d0ede8a28af40e70a28a2800a28a2800a28a2800a28a2800a28a2800a28a2800a0903a9c566eb1abae996f94412cac70a9b80c7b9ac59a4bdbfd39e77ba0f26328917080ff008d7257c5d2a0d29bd4b8d394f63aca2ab69d73f6cd360b81ff002d173c559aeb20286fba7e9456778826bcb7d0aea5d376fda11095dc322803cfbc2cfa75c5ba3cd121b8f39fe723a904fe7c56178eaf88d763b5f0f4cad35e29578e3e0249d01fc726b9fbc5b98ed1468baa34e04a64756b778b693c1196e08fa56cd95b6a37d64915bc965b70330210b827bf27ef5786e87b2a929ce7a3e9d0eb53e649450b61e1cbf92c5a6d6ef8dbeeea11cee63e99f4af5bf08c71c3e19b68e15015063818cfbd78509357324da2c6935e4b04df2b44a5948c0fe21c0af6ff00044a65f0b40af098648898a442c1be65e0f22bbf0aa766e6fd0caaf2dec8e828a28aed310ae43e27903c0f719ff9e8b5d7d721f13ffe447b8ffae8b59d5fe1cbd1933f859e07451457c19f30156fc38d6cdad5e4173fbbcc68c8e541dc72722aa540d2a5a6a56b74e32bb8c4d9e9f3719fc2bd3cb2a2862127d743b3053e5ab6ee7b9786ae7c3761a7c7bee6ddee09ce0af34be38bd8af7c38643e7c5146c4b0500312318ef5caf8652cad750864bd0b2c3d4ed889fa5696a56c9e30f1a59e996248b6b7713dc298caed507201cfae0d7d69ee9d37c2ef0f7f627865aea75227bd6f358b75dbdbf4c5713e39d69b5df168b0b790152ff67873d393890fd769af44f1c6b90f873c2ce88fe5c8e9e5c58edc7ff5abe78d375909e20875a791e58ada5c0f75cfccc7df1594e71a697375d089c947736fc51676da147fd9d6807daeefe569368dd1c6bc13f52706b0511638c220c2a8c0156351d50ebfad5d6aee1b333908186308381fa0150d7cce6589756af22d97e678d8cadcf3e55b20a86eff00e3c67ffae6dfcaa6a64a9e642e83f8948af321f1238e3ba3da3e1f4315dfc20b68a53b11a2604fa537c0ecd06acf1c8a77480a86f4039a77c21b8b7bef87515a160e619248a44ce71f311fd2ba236da5f87e496fa7b88ade35524877031f4afbdbe87d41a5a9eab65a3d9bdd6a57296f0a0c96735e2fe2ef8ad7be20f32c7c305ad6cfa3dd30f99ffddae77c5fe273e35f155ccf1cd23e976f848236042b1eedfa5678000c01815e363732f652f674f73cfc4e2fd9be486e470dbac5962cd248df7a4739663f5a968a2be727394df349dd9e3ca4e4eec28a28a8105145140051451400552be38bdd37febe92aed51beff8fdd378cffa5a576607fde61ea7461bf8d13eb91d28a074a2bed0fa10a6ca710b9f4534ea82f6332d8cd1a9c164201fc2803cb340f10dbc7aa5d595d2c66592e9e4dec07b7f8560fc45b7b2d77c4163069b03dccf19ff004a36ea0911fbf4f6ac6d574cb1b7b38ee2dae2e2df5188959212a584a4752180c0aeaf40d2340f10e931dd59cf736b2c0b89d376d6dddc9c8af02b53a742b7b76dd9df63b63294e1c884d375cd0749b24b7d0ade04db288d9f682d818dd93f4aedbe1bddc773a25d08c82ab75291cf505c9af1f8b40d3f54f11cd36986ee4b5cedd919f9a771fc5bb185edd6bd2be1b694b61abea2ab662c8c0154c7bc3336e19c923826baf074e14e774f56baeff333ad29496da1e8f451457a8730514564f882fe5b4b348ad3fe3e6e1c46847f067f8bf0a994945733d80d6a2b122d6a6b7ba482fe13e5b0004e9d33ee3b56d8208041c83d08a9a7561563cd0774371717661451456820a28a2800a28a2800aa5abdff00f666953dd05ded1ae553fbc7d2aed6178ac31b3b46e762dca9938fe1c1aceac9c20e4ba21c55dd8f37d4afa7d63c589a4f9cc42b892e9d49e58ff0fb0e2bd16dad62b4b55821184518009ae0bc3ab1e9ff00117538af4ec9ae242f0e7f8c62bd0ebe37195252924fb5fd6e7a7049219a0ce96d71369a5b050ef8d73fc27ffd46b76b96d46de612477b63817507201fe31dc56ae8daf5aeb10fc87cab84e2481ce194f7e3b8f7afa4cbb131af452beab7386b43925e46a5733e3ad71f47f0fcc96d03cf73708638510672c7a574d5c2fc4326e6ff0047b08e7f29e49c499079001ebfad77d49a845c9f4325172764721a049a6dee9acb7637aadc189581e9850c723ea6b3359f0edccde3743a34bb6c8c2b34a226c1381db1dce69ba8e95a843af4365637c920b8766900e082460b1f7c55b7d3358d26ea396c6da3730e23fb679abe63e3b119e9ed8cd78114949ce3537d933d1937cbcb289a5fd83aa476c65bad4c69f028c32dba00e47fb47839aed3e1d18e3f0fcb6d1cad379570e4bb9c9393debcbe3d5b58f145f5edbc98b389088e46fbdbcf07e502bbcf85fa6cfa63eab04ece46f42a1d81c75f4af4b0aeb737efa5f2392a285bdc47a0d14515e81805721f1433ff000835c63fe7a2575f5c8fc4f38f03dc7fd745e95956fe1cbd1913f859e05451457c21f32151cf109e078dba30c67d3dea4a2aa32719292dd0d369dd1d1e83ab245a4c2f70cc248be575ce391c2fe7c1af61f02e932e9da3c9ab6aea12fef7f7b3647dc5ea17f0e6bcc7e197835f5dd68ea376a469d6cdbb6f699fff00adcd6ffc65f88d6fa1e93369366cc6e245dade5b7af18afb8a32954a6a525667d2424e50527d4e0be2df8c27d7fc44ba5e9e5a58d8950aa71b547049f439fd2b91d2e5115edd58b44b1286dc899cf07b7e95d4782fc257906857be28d5ceeba9e2ff0047327555fa7e1587a84567a7d89bd4579efe471961d1573c8f7279a8c551f6d49c3af4f526b53f694dc4b2000300607a0a5a8ade75b885644c804743d454b5f153528c9a96e7cec934ecc28a28a910db496ff4cb879747d466b13272eb1f2ac7d71d2a2b98eef5090beaba95d5e7fb2f210bf967153d15d7f5caea3cbcc747d66b5adcc3638d224091a8551d0018a7514572b6dbbb39f70a28ab7a06912f892f2f52091a382ca0691a45fe260381fcaba30f86a988972c0da8d1955972c4a7914b4ba5c76d75e0dbbbbb8b576bc43e58b96e0a49ce00f5a626762eeeb8e6b6c5e0de16d777b9a6230ee8daeef71d45145701ca14514500159fa8ff00c7f697ce3fd312b42a8dff00fc7f699f2e7fd2d3f0aecc0ffbcc3d4e8c37f1a27d723a5140e9457da1f42151dc122d652b8c8438cfd2a4ae6bc6de228f45d1e5891667bb9a33e524513377c13c0a00f28d3bc4f05b5cdcc5ad431b4392c0941c9ef86eb56e4b78b5398ea4b1ddd969f2c26248ec9019261c6588c8e063ae6b521d2b4fb8d0d22b6bc82481b0ad0de213b59b8e83045673deea3e04b358ef6fa2bad36e1cc71bc6b8fb3b7a0f6e0d7cfcaa51e77282bcfb3baf53bd467ca93d8b5e16bfb6f0b58ff667923eeef86723e69077dde846338aea7c07aa0d575ad4ee6351b1ca82c0f7518af2ad5fc4173e33b2fb3e928efa9c537ee446bfc2d80413d0719aea3e19e8da9f86bc596b6b7d7e920bab795e48633c2b060304f73cd6f84a4956f6b2566fa115657872ad6c7b4514515ec9c623b2a233b9c2a8c927b0ae52da63abea92ea4d911464c56ebec3827f1c0356f5fbf96e2e1749b0702471bae1c7f027a7e3d29d1ac3696e91a958e3401572715e0e6d8ae58fb08eef73ab0f4eef998f9702262c3200248aade18d5566924b56327cc59a3471f7003cf3efdab0f58f14a44b2436bc30c832374c552f0f5db19ed2e64dd9babc8a38f775206413f4e95c3973a946a27d1e874568a941b3d3a8a28afac3cd0a28a2800a28a2800aada859a6a1a7cd6b2121655c647515668a4f55603ceaf3488350bd8ed358dd6da95a9260b88ce0c83d474cfd298d26a96ade5ff6f2ed5381e65b2e7f9d76dad68b06b369b25f92543ba2947546ae36f74e1334915f5ac475280756ce2651d08e7e95f378cc1ba2f997c3f7d8eea5514b47b8a8da95d4bb535e6e78f96d13fc69b37829ee6f12ee6d5ee16e17fe5a429e513f5da79acfb32f6f3285b5b50e0f3f3107f535d8fda24164922c60b1c06e7217debcde6ab4e6bd93d7e48d9c636d4c43e13b83ff0031ebff00fbf8dfe359f7bf0edaf2f21bb3aede7da210446ef97c67ea7daba37b8bd134914325acd345cb42a70c78ce073d6a0b3f107f685ac53595abc8cd3981e3dc014607041fc457555fed085a336f5f4338ba2f5471b2fc35d5edf571a958ebc64981ced962001fd4d52bed0bc756acf2c715b5c8de5c08dce727be315d7ead71abbdc978e18e148f0779955bcb00e49201f4adc8354b59562025dc6440e1829c118eb9a555e26828fb449b7e5b17194657b1e3b6f7b73e178035fd8dcdbdd4d36656957e5418ec6bbff855e206d72fb5770b98d5914367d335d4b25ade2956114c3b8e0d61dc7842cade67bcd19df4dbaebbe038ddf50735be1b31a719f3558d9f733a949c95a2cef68ae26d3c57a969bb5357805e418c7da2dc608fa83c9fc0575961a9d9ea502cb673ac8a46719c11f51d457d1d3ab0aab9a0ee70b4e3a32d5725f13467c0f739fefad75b5c8fc4e6dbe07b8f79145157f872f46673f819e05451457c19f3215a9e1cd06e7c4dad269f66b9ef2bf68d7d7f9d52b2b2b8d46fa2b4b389a59a56c2aa8fd4fa57a83eb1a37c26f0d7931b477bad5c2fce223925bfa0cd7af9760fdb3f693f851dd85a1cef9e5b2fc4dbf1378934af865e1386c2df6f9e536471af059bbb7e75f3bcc7ed9e288f59f14492496ef2655719d8c4f19fc6b52f6f2fbc41abc9abeb4fbe793ee47d905124692c652450cac3041ef5d988cd146aa8d3574b73a2a63546768ea8f58488f88bc30b6769c27037271f2e3b560c9e17b5d355aeb588c8483222800fbe47f4f7ae6fc31e2e9bc29790db5d06974f77003f78bdbe95ea3ace9c3c4f1472d9ce8612036e539c8f6fad7b34ea42ac79a0ee8f42138cd7344f1b961ba9efaeaf923458986f681140daa38c8a64722cb1abc6c19586411debabd67459c5c0d334d52de61dd3c8a3ef01fc3f4f5fa57357b61792ea412ccc71941fbc4da70c7ff8aaf371d8055fdf87c472e270aaafbd1dc8e8a63b4905c35bddc4f0ce8017423800f4e7a53810c32a411ea2be6ea50ab49da71b1e34e9ce0fde42d145233aa901980cf4c9ac926f62126f6169aeeb1a16760aa3924d40b7667b836f670bcf2a9c100600fc4f156b4dd0ae2ef5792d35f465990ed5813ee10475f7eb5e9e1f2dad55de7a23b28e0ea4dde5a21fa5695ac788609a7d1ad55ede22034aec41fa818e6bdb3c1fe18b2f0cf86fcbf93322ef9dc8ebebf8561f84345974192596fa6486da15015998282be86b03c65f10db595934cd05f6d983b66b8c11bfd97dabe8614e8e12936b447ad1853c3c2fb232fc6bae5a6ada8ad868f0a43a6dabeedd128512c9ebc7a7f5ae7a91542a80a3007414b5f298ac44b1153999e257aceb4f9828a28ae5300a28a2800aa37e717da6738ff4b4abd552e63f3354d297939bc5e077aecc0ffbcc3d4e8c37f1a27d6c3a5140e9457da1f4215e6df12a6925d7b4cb5b4bb304a10bb6d6c1db9ffebd76faaebf61a3c79bb9817e8b127ccc4fd0735e6fac787b54f19eb8751c7f65dbe4052c73230c63f0ae5c557851a6f9a566694a2e52d8c1f13e892ff631d4eeb5986d9e1512c6b08004c472012319ae6e2d76eee745b684da8d71c3993ec68b950492725bae79e98af5ad37c01a2d8c682789af5d47df9db3fcaba04b7b4b550a9143101d3802be71e6169737c4fbb3bbd9ab5b6478c7853fe127d2a4bf96cbc2335a8bd538c0c94623a86c671ed5afe1e83c55a76ada7dd5ce8d2bf90ede631072431c9af531716e3004d1fa01b8524d7715bb2ac85b2dd02a939fcaa3fb42aba9cca3afcc6a9c546cd9147e25d40b7ef34891571d8934cbbf14dfc3692347a44cd36df917d4d2bea91229668e6daa0927cb6edf8550d4b58b5fb32491c4d396195c718ae9fed2c647e28fe062a8d296ccc6d3f5c7d395a4beb2b8fb65cb169a475c0cfb7b5665eebdab5d5e15173a78899b016493e6fcb6d599b58539dd6b3c4a46415ebfad58d0e27d72e0c8935da5bc446e12aa804fb715cb296aea4d6acde2acac8af078724d41fcdd46596741f3841188e3ff00be81e47e157bc3764dae78c22bd8a4cd96960a0d8309bf23803a1c60f353ea57771e20d4bfe11fd0db08a07daee57a22f751ee7fad771a4e956ba2e9b15958c61228c63dd8fa9f7af532ec3ce4fdb54f9239ab545f0a2e514515ee9c81451450014514500145145001585e27b22d6d1ea30a933d99df81d597baff002adda4650ea558641183513829c5c65b31a6d3ba3cdb56b082e2e21d420488c530ce4bedc9ade5416ba136c18c2671bb3fad65de5afd912ef4962a1a27f32dc37431e7ff00ac6ac476971a978785b453fd9a4ddcb2f391e95f1f522e954519bd22ff0003d2e6e686853b86b68b5ed5eeb44469f54b7cc8f1cbf2ae768c9079cf1db152f82ada3fecfb4952612cb71746ea5238c3124b0fc09c547a7f86352b3d4af6f5f520f2dd649f9780718f4f4a8745f0febde1fb832daddda5c26e7758e556c02c72718af5aa6330f5651b4ac93bf5d4e554e714f43124fb35eeb1e4586a53c91dedc4825f9366d1b71d41e4645758eb3e8de1ebafb0db8bb92d2048a2529919dbd48fc2b9ed674fd4c69ad6f67e1d8d1b7ab7996d2600f9b27ab67d6a79b55b7bcb7d37487967b799ae03cc7cb61800f0a4e31deafda4255bdaf326927a2d41c5a8f2a42df5fdc47e156d7bc95b1bab59d5488b88e752403c703b9fcab55759b98fec714a16692ff73c4a07cca831ce3d391556f7c2f03cd02dddf4f745a41e4c7211b571c9e001db3525837dbf58bbbb45daeb2fd8ad57fb8149527f1e2b1adec3134fda463aecba5d951e684acd992fa9cf0b48ec370079743c75e9fecd4b693c373324f653b5adc03f2c9171b8fba8e187b9aaba86a5fda1e2b960b6406cb4c5f297fbb2b9192c7d7ef63f0a92efc313c1642f2dca995944af0c6df32af5e475fcaa161a709bf64f58f5468e71945732dcec74cf15b44045af2ac3ce12e97fd5bfd4f63ed5278f2d4dff0081effc9f9cc7119940fe22a09ae4b4b9de48cac80b86e0861f7be9ed5b30c37ba65a1974f1f68b661f3d939f948efb73cfe66baa8e651bfb3ada3ee73ce83fb27858ce06e183dc7a556bebf86c210f3301b8e141ee6b475258e1d6ef2de149235572caaea4119e4fe44e2a81b281a5124881e41d19abc5953a74eac955d96d6ea7cc4a9c69d46aa742fe9fe2f9ec74c683c376c45d4fc4b7f20c617d17d2a84369b676b8b995ee6e5fef4d21c93fe1538014614003d852d5e231d3ab1f6705cb1ec3a98994d72c74414514579e728d745910ab80ca7a823ad69f87bc47a9f856e3360fe7da11f35ac8dc0ff77d3e959d4574e1f15530f2bc19b52ad3a4ef13d7f40f18787f5e4f2edde3b5d41948304a007e7ae3f1a22f0841676bf6e9dbcc99732b803ef1ea2bc6e4b68a56569132ca72082411f956be9de26d7b49016cf506922ff9e33f2bfe35f434735a335efe8cf569e369cbe2d0eb741f0ddc5e6a8f7facc6248e6919ca38c8d99e063e958f6fe1b8356d66e22168c904d2175f29ca041e831f4abb61f162e911a2d5745057a7996cc003ef826b5f4bf899e17b68951a396dc0181ba324fe82bd155294d6e9a3af9a125bdce323f035b36a735ac6d77214971febd86d5e6b453c1d1d8ebc6d2188ceb959479dfbcda3d327dc56dde7c46f0b5a5e49736b1c9713c8b8c244c3f98ac5bdf8a9712b89348d04efe824b861803f039a9e7a3057ba426e10d5d91b37be11962f12a4b6f12c365788a1c22e023818ff001a9bc45abf87740bab5b99ae84da940bb04080333fd466b85d43c4fe22d5c9fb5ea1f67889e61b6185fd79acc5823590b85cbb7562727f5ae0ad9a518690d59cb531b4e3f0ea6af887c4da978a6622e89b6b1fe1b643f7bfdeff000acc4458d0246a1557a003a53a8af9ec462aa621de6fe479556bceabbc828a28ae6310a28a2800a28a2800a9b41b66d4fe2168560abb879de7371d8022aacf3c76d03cb3305441924d765f086d21d352f7c63ae7c8d3feeace1c12ccbea075ea2bd8cae93751d57b23d0c0d2729f37447bdc922451b4923054519663d00ae4efbc4d73abb1b5f0caee8f9592f987c8bfeeff0078fe558d757f73ad7fa56ba5aded4f3169e8793eee477fc697fb61fcb4b7b48e38140fbaa3b7afd3f5aeec5663bc286afbff0091f454e8df591b1a76836f64de74c4dd5d9e5a797e6627db3d2afdc4de45bbc9b19f68ced41926b9f9ee4436f6a9777b72b34e331db5b01bb67a9c838aad34b7be1ef2f504bf9b50d2e47093a4e007833df8038af2ea603112bce6eef7b75365562b44b43420d6ee9b5cb5b7b98962b6bb88bc0e0e77900923ea38a7dcc3fd9f6ba9cf1a3dd5c2a79d024ac580e991cfd6ab788ec64bbd299ed0e2e2c1c5dc0c3ba641641f82feb57ec75cb295ecaedae6143709e5b2b38182464e41f715d3469c29d7a7382f766adf322526e0d3dd1977da9585d7856e26bb8e28ddc05b59628c2334bd9463df15b3a6c730b7d2cdd3113ec4473df3b79fd6b9dd3f51b5d56d351d3f5c9c5e431cae18a8c9848190cbb7afebd2b3f43f16c904f6f65756d773c569700a5d0898092319c7046738c574d6b3ab4e49df95ebd1930bb8b56dc875a2ba6c4cebe2792f2ed6e5227b78ee989019803919ec0d6adccd69630224176b205272e79e7d3f0a89aeb4b173753da689a84a6693cd64f91416fc7e95ce996eecf4b3bf4c63ba79242bbc6543312075ec0d4637f7b25ef69daf72a8c52d91725b937f791c1677064767c633c015d4ebd7b2e85e1d8ad2c07997f704430a8ea58f7ae7fe1fdb417d7d717f241244d0f00484601fc3eb5b7a0c5ff0927c44b8d4b779967a52f9310ede61ea7f02b5c94a82ad8854fa2d59ace5c903a8f09f87a2f0f68b1c0bf34f27cf348472ec7d6b728a2bea5249591e70514514c028a28a0028a28a0028a28a0028a28a00cbd6bc3d67ae46a2e4324a9f7258ce187e3e95ce1f00deab1f23c433c4b9e1553ffaf5dbd1594a8d39bbc95ca526b63895f015fae71e24bac9f504ff005a63f81f5b8d736fe2694b7a3c591ffa157734544b0b45ef1435524ba9e7b369be3cd3c6e825b3d411464860118ff3aa8fe2d6b30bff00096e873d90cf1298f727e640af4da8ae2d60bb88c773124a8460ab8cd7354cb70f35b5bd0b55a699e7daa5edaeaf6915ee8b72259223b95a37e13eb59727896fe2859ad34cb58aee6041b9f378527ab05c7e35d36aff000d34db9633e8923e9575d774272ac7dc1cd725ab58eafe1d19d6ec45ec0319bcb71d7fde1e9f415cd0a15f091b43de468e54eafc45ef0d69b6f059c97f2389adacb324a473e6b9edf99abb1eaf6505bb78ad6194c923889adb1bbe63f7467b0ac9d3d7cc45bcd06f154c9cb81cacbecc0f41f9526b77b7d7d0d96913c1656504b70af98b70dcc0f1924f039ad70f5e9f2f2af8bb3ddb154849bbf43434a177716e07ee63f24b4d312388d4e4e33f88ae8f47d521d574f8e78411b8743dfdc7a8ae635685edcdbf876c43341c3decff00f3d4f6507d3a7e55d05bdaf916e2da17fb3ee5c8db8fdd01d4d7958da11738d386b37bfccd69c9db99ec55f12f83b4ef11c0c655f26e40f9668f83f8fa8af22d77c3ba8f876e4c7a845fba27093afdd6ff0003ed5ed5a46abf6f6911519e388ed5b8e8b263bd5dbbb2b6d42dda0bc852689860ab0af39c9d397b3a9adbf0f433c4612962a377bf73e75a2bd0f5df858d1c8f71a0ce7cbe49b57edfee9ff135c15dd9dcd84e61be85e0957aab8eff005e8697b3babc3547cd623035b0eeed5d7721a28a2b338428a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a2aadd6a369669bae274519c633939fa0aa8c253768ab8e31727648b555ef6fa0b0b7335cb855edea7e951587f6d788a6f23c39a5cb3027067906147bf6aefbc35f04c1956f7c5f77f6d97a8b75e117eb5d6b0f0a5ad776f25b9ea61f2dab51de7a23cd348d2f57f1e6ac82d2dfcbd2e170d24b270981dc9fe95ecf616d158c31884f9af0a8512c8b8118ff647f08f71fd6b66f34a8749b28e1b4f2608b2163b78877edfe4d65c5633bdfdcc1709e5cb6acae2d89f9654201c9ef8c9ed5d3ed9d585a0b961fd6e7bf4e8d3a2b962125c07567858be2408f29e4f5e7f114cb8b3fecef15cb6d117912489244909fba08049c7a669750d246af7067d16e059dc3124e9f31f9243dca9ebcfb9abde24827b2d6b47ba9e32b33d9f9279e036471f90aeb58654e8c9efd995ed2f25634eeed6f06a90eb7a5dbfdb3740b6d710afdf8f6e7903fe05553c4b34c3439b4fb94097dab3ac70db8392a07393e9d2abaea521982a236eddcca870507707b1e2a4bed5b42f0e482708d79a8cc3288a4bc8dfd07e951f5983973c60dd46ade42e4925cb27a1a9a8e93a85dc76d159ea1f648e352b210996604608ce78ae72e749f04f8613fe26970924bbb7113c9bd893cf00d5d5d1fc59e308d5eea7fec1b16e7cb4c195d7f515d0691f0e3c3fa5a8692d7edb71de7b862cc7f0ce28a180c44a0a339d976412ab04ee91c8e9fe2dd214ff00c529e1db8b862482d05aedcfe22af2ebbe31b97c5bf859a25ec66908ff00d96bd1a2b6820188618e3ff7100a96bbe395e1d7c4afeacc9d79f43ce2e6d3e205e42516c74f894f506e39ff00d06b3e1f0778da793fd2dac224cf23224fe95eaf456d1c0d08ab2893ed677bdcf3ab6f875abca192f75bfb3c2c7e68ad21f2f77e20d769a1e8563e1ed3859e9d1ec4ce598f258fa93deb468ae8852853f8510e4dee1451456a48514514005145140051451400514514005145140051451400514514005145140053648d258da39543a30c3291c114ea280387d67e1f247235f785e4fb0dce773403fd54bec476acb86f6cafa6fec8f1569eb697c781bc7cae7d54d7a65666b7e1fb1d7acda0bc8fe6fe09578643ea0d70627050afaad1f73685571380f10daeb3a758ac568567b404037046658933d01ebf8d5d7985eda5be93a5dc79e67456bdb90fbb6c7fddcf7c8cd1f69d47c2928b1d7d0dd69edf2c778a3381e8c3ff00ad5a1a7e8da74725ddee94e36de200421f94633c8f4eb5e454c4d5c3a946aaf7b65236508cf58edd889ee61b0820b6f2a50ce36dadb44db4b01fc4c7b0c835235f496d6af79344f6ef13289a1326f5dac7a83c7bd67ea56d3dfccba86977263ba860f2046c546c6df9c9dddb1e9eb46b17de7daae930b24d7770cad72e9cac31a9e79f5e4d76cb0f08c6ce2b92dbf5bfa99a95defa9d19bfb65281e6443228650c71906abeaba169dadc023d46d92603eeb11cafb83dab9fd544f746c749830af7444970c7ac70af1f864807f1a7eb134d6b0daff665dcb0cf712ec8225c10635c658e7d8d78ff00519c64b9656695df91d3ed13dd1cdeb7f0b67814c9a14fe6a8c9f2663c9ff81579eea1f68d1ee160d5ed26b4909c65d08527d8f7af7eb29ef85a2bdd345286380bbb6bb63a9e7029f7965a66b9035adfdbc72e4728ebc8fc6a2519d3b3af0d1f5470d5c0e1abb6d68fc8f9f23b98656db1ca8ede80d4b5eaf71e19b3d163f2aff4c8b53d2f764151b65807d46323f3352c9f08742d4a1179a1ea135bc720caaa10ca7f3c9aed865d4ebc39e84fef3caab96b83b26791d15d8eb1f0bfc43a5ef6b7893508c723c838207beec5727716b7169218eeede485c750ea463f1e95c357075a97c51382a61aad3d5a22a28a2b90e70a28a2800a28a42428c9200f5346e02d1550dfac9218acd1eea6ce3644a4f35d4e89f0a7c57e254325fba6916acb901fef38f6c6706bd1a397d6aaf5d11d94b0756a6af4473173a85a59e7ed33c719c67696e4d436d777faa90341d2aeaf73c6e084283f5aea6dfe1368f2f89da3376f7363a7102e6690ff00ac7eea3e9835ebba29b18628edb4fb68a089061405c1c5557a7430af96dccfaf647af87cb293579ea78de9bf0bbc65acaa9d4e78b4d858e7e43f381fa5769a0fc11f0ee97209f52f3351b9ce4b4a7e53f8735e9548464107a1ae49632ab5cb1f7579687af4f0f4a92b4519d6d269ba6442d74e8540438f26d93247e029973aea5aa96bbb2bd822e865780855fa9ed4dbab2921d2351b2d28f9171344d240e396dd839193f8565f83ca3c36f18ba6ba82f77457114c776d619cb7e62bbe8e0a84e1194a4ef2fcc975269bb74207bab3bff136971d9dc0b88dd2659407ddc0036b7ea6b4b5eb39e410ea964a4dfe9c36c883fe5bc3e9fa8fcaa1f085b416dfda56d046a63b4bd96285c8e768380335a1abc7aacb750269263854e44b33f65f4028fac469e2254a6bddb59fcba8b91b8a92dcc878f4bd5edd2ff4c656764ce40f9a3fc7f86aa6a8b7379e4b6a777badecc6f1bf803fde3dfad5bdfa2781eca425ccf773e4b28e5e66ff0074703f4aad61e18d5bc6f751dff8801d3f4c56cc764bc3483fdaf6fc68c253ad51b8d36f91f56139463abdcceb49b53f1249f64f0a438801c49a84a3e51feefa9f7cd773e19f0269de1fcdcc99bcd41f992ea6e5b3ed9e95d0da595bd85b25bd9c290c483015062a7afa0a1868505eeee724e6e5b8514515d24051451400514514005145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514011cf0457303c3708b246e30cac3208ae2effc2d7de1f99afbc2ec5ed87cd269ec783ebb7d3e98aee28acaa528558f2cd5ca8c9c5e8794ead3c5e288becd6d0c105e3e1278ee8059139cf19e4f4aea1348b5d3f4b686c2d634f97e6445c6ff006ad6d67c2fa66b6b9ba802cc3eecd19dac3f11d6b9db8d07c49a27cda3ddaea500e7c9b8e1be8081fd6bc5c465f5ad18d27751d91d30ad1d79ba90cad66fabcba8aea90da799108e5b69c6245c6385c9e9c56769aefaf788e6bb8a265878b7b488f0638c672d8ed907f4a95fc551d94aa3c55a13e9f231c09648d594fe2326ac456ba46a77cda8687a91b6bb6182f19ffd95b8fd2b2a989b4b96b53e54ed77bdec5c61a5e2efd8a5ad6a3a56ad752482d2e31a7a98229e3f9d4b0ea7601ebdf356fc2161aa32c7a8eaf7724c1e21e4c726731e7a834cd42cb5748cc1f60b7bd32f02e23250afbb01815d2d85aff67e9d15b990bf94982edd4d658fc5f34396124d3eddbcc54a9abddad89e5645898ca404c724d56f0322a6977a22ff00526f643163a6dc0c63dab9cbfb8bcf1278822d2b4d256dd0e67957b0af41b0b1874db18ad2d976c712ed02bb329c34e9a7565d48c44d3762c554d474ab1d5adfc8d4ada3b88f9f9645ce2add15ee9ca79478a3e126c56b9f0dbf1deda43fc8ff004af30b9b59ecae1a0bb89a1957ef238c11f857d4d5ca78d3c1369e24b179a28d63bf45263900fbdec6bcac665f0ac9ca1a48e3af84854578e8cf9fe8a96e6da6b3b992dee5764b1b1575f422a9c715f6ada945a6e8d134b712b60b01c257ce53c354a957d925a9e3c28ce53e44b523bbbf8ed7e400c931e1634e4935d5f857e156bde2b2b75ae31d3b4f241117f138ebc8feb5e93e07f853a6f87234bcd4c2deea27e6677190a7d00af42002a80a0003a015f4f84c0c282bbd59ed51c34292eecc0f0e782742f0bc0a9a559471b8eb2900b1fc6b7654f32174ce372919f4a7515e82496c751e676ba70d39ae746bc1e4dc3dc3cb03938138639e0faf4fcea7b490dadd26479610e307afe35d8ebda0db6bb63e4cf9495798a65fbd19f515c6a47713c73dbde4656fec4ed381feb57fbdfafe95f398fc23a72735b3fc0eca534d729d523ac8b943914eac7d0aebcd8d93a7a0febf4ad49ae21b68cbdc4a91a8ea58e2bc0945a763ad3d0a7ab6a30e97145753248db64007968589f6ac0b5b79a49256f0c69a74d4b824bdcdd0f9973d76a6011f9d3f55f881a25ab882d8b6a3396c08a14ce0f6e4f14d8ed7c6be24e891e856bd72dcc8cbfa8af6b04b17ecf929c6de6ce7abc89de46ac52e97e15d2c4573771c40659da471ba463d4fb935892ebdae789e516be13b378603f7ef6752ab8ff67d7f3adfd2fe1c69369279fa8b4ba9dc673e65c31ebf4e95d6450c50462382358d074551815df432a82973d57cccc675db5689cb7873c0363a44a2fb5066d43533cb5ccdce0fb7a57594515eca8a8ab239af70a28a2980514514005145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514005145140051451400514514005233aa21672155464927814b5c978eb53305bc361bfcb8a70cf3c80e0aa28048fc4135139a845c9f41a5776284edff000976bc277c9d2ac5ff0074a7a4d2742df41c8ab575e18d26ee4f3cdaac53f69a31b5c7e35c18f8b767a7bfd8ed74a97ecf10c065c648f5eb5a96bf1674bbbc32af948dc012f0c0fe15f278a8e2eb54f68d347a14f9231e53a3b596f348bc8ed6fa5fb45b4cdb6198fde53e8debf5a8fc59ae47a5e9aea5c2330c124f4cf03f3a81b5482f84579a85edac76917ef1551c924fbd79ef8975d3e25d5cdbd9fcf6c8fc3e3873e9f4f4ac28d1e7a89c968b7347b591ec9e0dd3ad2c743592d2649dee3124b2ab03938e9f874ae82bcd3c31e1d9ecb4f592cafa7b6940c8e772fe478ae961d6358b018d42d05ec609c3dbfdf3f50702be928e618697b89daddce09519ad773a6a2b0edfc5da4cb27953cff669b38f2e5183fa715b293c4ff72446fa3035e8a69aba311f45145303cbbe297826eb519e2d4b42803dc484473281d7d0fe18ae93c09e04b2f08e9a8422c97d20ccb311ce7d07b575b4542a7152724b5625149b6ba851450485192703d4d58c28aa777abd85847beeeee28d7d4b67f95634be37b17729a5c13ea0c38cc0bc0fcf15329462af2760ddd91d2d713e31d4acf4bf1169570d748b2cac6de58c30cecc12323eb55f57d5bc47776a4b98b4c80f04a7cce73eb91c7e15e73af585dab09149de48259c966720e4104f4e95e757c550ab074d3bdce8a74a77b9d6f8afc48be1176d9ccd3366151dcff9fceb9fb1d1f55f1b4c66d56f1a68bef792cdf22fb6ded58de257d47c4896da9d8c2b75796d188a7b673865207de03a60e2af786bc41a8e8fe5cfab5b7d95138c4a401ff8ef26bc98d370a5785b9ce94f5d4ed2c34bb7d2addac56ce385e33be3f2d31b8afbfad7a269b7915f69f0cf036e565c7e2383fad79a5dfc4bf0e4ce6d84e525c6449b78535d1786affec5a8456e24596d2fd37c2ebd03fa0fc0135d596d49c26e1515b9bf331ae9497323b3a28a2bdf39028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a002b84f89765235a4176aacd000d14d8e76ee0006fa0e6bbba64d0c7710b453a2bc6c30cac3208a8a90538b8b1a76773e5cbfd224b7be0597744dd2403861f5a86e34f548d99307775e718f41f87ad7adf883c1f77a1bc971610ff68698c4b3db919787dd7d47b66b0f45d2744d6afb10dc2a6cc3ac5270e3d88e95e2d694e8fc6be677425192d0e1b4af0cddea1731ac298c9e170715e9fe19f0335aa89ef30ac3eec7d71f5f5aebb4dd22d34e8152de35cff7b19357f38eb5e356c64ea691d8dd4558a52433c71416f60f1c72bbe3732e4018f4a89f537b452d748b2c4a70d3dbb0700fb819c7e350ead7e2d74dd42fe3c96822f2a3dbfc4cd8e9fad41e1bb186c21b78ddcb05b67fb564e5586d3c9aeea184a32a508d44f9a5f81cf29c936e3b235274b0bd8505c0864490654311f3551ff845ec2393ccb169ac9bb1b77db8fd2b3bc1cc2e3480f3c66558669e48f3dd048401f911503789ed1e21f66b6b9b7bd76185c975c67be4d4c3055a1172a752dabf2d86ea26f58dcda161acdba9fb2eb73393ff003f397ff0a5f33c5b1c785d434f908f5b66c9ff00c7ab4566d96a924dc12a09fae2a3692f030c45126ee88ef863f85450c563e77f66dbb795c270a4b7d0cffb6f8c31f7ec33ff005c8fff001555ee358f145b38173a86956c1871e645ff00d956adbea50cf35c40d98e5b6dbe6a37f0e464560ded8dd5e5e4b736810c32f4fb428218f4f9739ae9a18cc655a8e1276b7919ce9d28abfea5b0be2bbc024fedcb211b0e3c8b761f91dd47fc23f7f7285351d7afe546fbc892ed53f862934078b4c87fb2a512c7763749e5cdfc5939257db9abf06a31dcddc76a6f6d6396407088c4b71f863b565ed31f5aa3a706f4f917fba845368ad6de11d1ade4f30da2cd27fcf497e66abff6ab2b4531a3c60aff0002104fe55cfdc6b665d5adff00b296f2758ae7c8b891900423386efdb15ad7365326957a9a4a2ade1659158a066db900f5fc6b35839d4afecab4eeed7eff0021ba89479a284b8d46dee5a3b59a2923f3f2233321504fa735ceea9a53cede5c819ca1caa8f9593dd4f7a541a7ff00c249696d7733cf2b069449e7b9f2d971d41e075ed5d46a50996d418f3c0eaaa0e47d7b5615a0b0f554629af52a12725767926bda6477570c5996dd972a5d5087c0ea4af527deb0ed6cd6ce77485577b0f96493f78f8f5e3eefe35d5ea48bf6e7de0b229cb043bb38ec58f233ed583716cc66c8cb461ba20c063e9bbad775393b599b58c3d5a276949df90dc819cee3eacdd2bd2fc0715ecf6ba0c324a1bcbb969946dc623dac3a7fbc6b1478650c715deb20c71b30296b18e5fd147afe35eabe0ed0a4b38db50bd8561b89902c710e9147d87e99fc6bbb0f1f6b38db689c95a491d4d14515ec1c6145145001451450014514500145145001451450014514500145145001451450014514500145145001451450014514500145145001451450014514500145145001451450014514500145145000467ad606b7e0cd1f5d264b9b711dc769e2f95c7e35bf452714f71dda3ccae34df17f8525df6f27f6ce9ca7943feb147b73cfe5572cbc69a4ea60da5f092c2675c34574a531f89c57a0d666a9e1dd2b598caea36514a4ff0016d01bf3eb5e66232da355f32d1f91b42b38ab338e5f0edcdb44dff08e6af881c9630ca7cc8c9fc3155e5d3bc4474e9a0d42e6d60b1db9992ce260f20ee0727a8ad493e1bada49e6e85abddd915e562663221fccd5097fe139d1f225b183564ec61386fc80ae6951c7d35684aff9fde5f35293d512e9d7d6b71e1fb98b43ba4b2970b1c293b88980039e1b1e94f97169ad5a5d5dc8f3bc9fbb8d44ab260f73c7d6b2a4f15693e708bc41a23da5c772f00383f5eb5a5a6de785a5d416eacaf2369d41daa64385cf5c0e82b9ab5592a3eca5092b2ef74fd4b8c573732674d843756fe6769015fae0d60e930dafda2fa5bf2bf6c33912191b04703a7b56d3186e22c6f0558672ad8aab716124ee584d1ee3fc461527f13deb928d6a6f0fec272e5d6f734945f3f3257332ce0d3ed752bc586732bdd3a34cae7a200471ea39aa56861baf11eac7582be7433ec8165380b160118f6c935b706871ade1bbba93cf9fcb31860810053db038ec2abea5a6df49207b68ad2e08180d300ac07b90093f8d6d0ad46519d2e76afaf33ea4da5192972dfc8ced6a4458f469a2ff0059fda291c3fde3192770fa702b79ace0b1b756b58d616f317240f523359f61e1f9df518b51d6ee1679e10443122858e21ec07d0738abdac580d4add62fb6b5b2860495ef8e6b3c46260ebc1c1dd46daf7b15183e577ea51f0f45f63d5758b1fe14b932a8ff007c93572fb54d36d6ebc8bcbc862fb446d1480b8dc1704f4fad63dbd868ba2ea8fa8dceaeed70c007324c7071ed9c5437fe29f09497225f2e1bf9d7a18e10e49fc69c66feb7ede945b5e826af4f95b2382e34f82cbfb3f4db1bcd5368da014314647b961cfe75d16896f7716810c1a826c9d54e406ce392473f4ac01e24f105f0d9e1ff000dc9e574124a36aafe59ab70785fc5bac1ceb5ab47610b0ff576aa091f8e057555c2e231565c8a2bf13353843adccdd5b4bb086f1ae752bf8a18d58380ee1a4247a63fc2a0d3d65d4a558bc2ba63b3038fed0bc5c051ea01c66bafd37e1d68965309ee524be9c7f1dcb971f912457531451c1188e1458d1780aa30057751cb92b7b4772655dec8e7f41f0843a64c2f3509daff005023999fa2ff00ba3b0ae8e8a2bd48c631568a396ed85145154014514500145145001451450014514500145145001451450014514500145145001451450014514500145145001451450014514500145145001451450014514500145145001451450014514500145145001451450014514500472db4332959624707ae56b12efc11e1ebc56f334c84337575041adfa29349ee3bb38e3f0d34956dd6d73796fed1c83fc2a393c03771a8163e21be8cff00b6c0ff004aed68aca5429cb7435268f3f7f02f88ff00e59f8a651fef2d22f817c4c08dde297c77c29af41a2a7eab47f957dc3e79773845f00eaf2295baf13de1527f8081fd28ff008555612b6ebdd5351b8e72434a39fd2bbba2aa3429c3e140e72672b67f0dfc336a72d60b39f594935b967a269b60a16ceca1880e8156af515af2a26ec40a17ee803e82968a298828a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a0028a28a00ffd9	2020-01-22 21:46:35.978315-08
\.


--
-- Name: image_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.image_seq', 1, true);


--
-- Data for Name: instrument; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.instrument (id, scrip_id, name, description, instru_type, is_active, location) FROM stdin;
\.


--
-- Data for Name: instrument_analyte; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.instrument_analyte (id, analyte_id, instru_id, method_id, result_group) FROM stdin;
\.


--
-- Data for Name: instrument_log; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.instrument_log (id, instru_id, instlog_type, event_begin, event_end) FROM stdin;
\.


--
-- Data for Name: inventory_component; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.inventory_component (id, invitem_id, quantity, material_component_id) FROM stdin;
\.


--
-- Data for Name: inventory_item; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.inventory_item (id, uom_id, name, description, quantity_min_level, quantity_max_level, quantity_to_reorder, is_reorder_auto, is_lot_maintained, is_active, average_lead_time, average_cost, average_daily_use) FROM stdin;
\.


--
-- Name: inventory_item_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.inventory_item_seq', 21, true);


--
-- Data for Name: inventory_location; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.inventory_location (id, storage_id, lot_number, quantity_onhand, expiration_date, inv_item_id) FROM stdin;
\.


--
-- Name: inventory_location_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.inventory_location_seq', 20, true);


--
-- Data for Name: inventory_receipt; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.inventory_receipt (id, invitem_id, received_date, quantity_received, unit_cost, qc_reference, external_reference, org_id) FROM stdin;
\.


--
-- Name: inventory_receipt_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.inventory_receipt_seq', 20, true);


--
-- Data for Name: label; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.label (id, name, description, printer_type, scriptlet_id, lastupdated) FROM stdin;
62	MOLECULAR EPI - TEST	TEST LABEL FOR MOLECULAR EPIDEMIOLOGY	P	13	2008-05-01 21:25:56.575
65	labelname1	labeldescr1	P	12	2006-11-20 15:31:47
68	testa	test	t	11	2006-12-13 10:06:03.873
69	testb	test	t	12	2006-12-13 10:05:58.435
70	testc	test	t	12	2006-12-13 10:05:43.107
60	Diane Test	Diane Test	P	11	2008-05-01 21:26:17.387
61	VIROLOGY-TEST	TEST LABELS FOR VIROLOGY	P	12	2006-09-07 08:06:40
71	12	12	1	11	2006-12-13 10:56:53.37
67	test	test	t	12	2006-12-13 10:05:04.56
64	NO LABEL	NO LABEL	P	\N	2006-10-25 08:09:35
66	Label Name 2	Label Desc 2	P	13	2008-05-05 23:13:30.414
1	aaa	aaa	\N	\N	2007-10-11 09:33:32.059
2	a	a	\N	\N	2007-10-10 16:45:24.842
\.


--
-- Name: label_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.label_seq', 3, false);


--
-- Data for Name: localization; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.localization (id, description, english, french, lastupdated) FROM stdin;
1	Billing reference label	URAP Number	N° URAP	2020-01-22 21:46:11.017212-08
2	Site information banner test	Cote d'Ivoire Regional Laboratory Laboratory Information System	Système d'Information Électronique de Laboratoire - Côte d'Ivoire Régional Lab	2020-01-22 21:46:11.082808-08
3	test name	GPT/ALAT	Transaminases GPT (37°C)	2020-01-22 21:46:11.21754-08
4	test report name	GPT/ALAT	TGP	2020-01-22 21:46:11.21754-08
5	test name	GOT/ASAT	Transaminases G0T (37°C)	2020-01-22 21:46:11.21754-08
6	test report name	GOT/ASAT	TGO	2020-01-22 21:46:11.21754-08
7	test name	Glucose	Glucose	2020-01-22 21:46:11.21754-08
8	test report name	Glucose	Glycémie	2020-01-22 21:46:11.21754-08
9	test name	Creatinine	Créatinine	2020-01-22 21:46:11.21754-08
10	test report name	Creatinine	Creatinine	2020-01-22 21:46:11.21754-08
11	test name	Amylase	Amylase	2020-01-22 21:46:11.21754-08
12	test report name	Amylase	Amylase	2020-01-22 21:46:11.21754-08
13	test name	Albumin	Albumine recherche miction	2020-01-22 21:46:11.21754-08
14	test report name	Albumin	Albumine	2020-01-22 21:46:11.21754-08
15	test name	Total cholesterol	Cholestérol total	2020-01-22 21:46:11.21754-08
16	test report name	Total cholesterol	Chol total	2020-01-22 21:46:11.21754-08
17	test name	HDL cholesterol	Cholestérol HDL	2020-01-22 21:46:11.21754-08
18	test report name	HDL cholesterol	Chol HDL	2020-01-22 21:46:11.21754-08
19	test name	Triglicerides	Triglycérides	2020-01-22 21:46:11.21754-08
20	test report name	Triglicerides	Triglycérides	2020-01-22 21:46:11.21754-08
21	test name	Beta HCG	Prolans (BHCG) urines de 24 h	2020-01-22 21:46:11.21754-08
22	test report name	Beta HCG	Béta HCG – 24 h	2020-01-22 21:46:11.21754-08
23	test name	Urine prenancy test	Test urinaire de grossesse	2020-01-22 21:46:11.21754-08
24	test report name	Urine prenancy test	Test Grossesse	2020-01-22 21:46:11.21754-08
25	test name	Proteinuria dipstick	Protéinurie sur bandelette	2020-01-22 21:46:11.21754-08
26	test report name	Proteinuria dipstick	Protéines	2020-01-22 21:46:11.21754-08
27	test name	White Blood Cells Count (WBC)	Numération des globules blancs	2020-01-22 21:46:11.21754-08
28	test report name	White Blood Cells Count (WBC)	GB	2020-01-22 21:46:11.21754-08
29	test name	Red Blood Cells Count (RBC)	Numération des globules rouges	2020-01-22 21:46:11.21754-08
30	test report name	Red Blood Cells Count (RBC)	GR	2020-01-22 21:46:11.21754-08
31	test name	Hemoglobin	Hémoglobine	2020-01-22 21:46:11.21754-08
32	test report name	Hemoglobin	Hb	2020-01-22 21:46:11.21754-08
35	test name	Medium corpuscular volum	Volume Globulaire Moyen	2020-01-22 21:46:11.21754-08
36	test report name	Medium corpuscular volum	VGM	2020-01-22 21:46:11.21754-08
37	test name	TMCH	Teneur Corpusculaire Moyenne en Hémoglobine	2020-01-22 21:46:11.21754-08
38	test report name	TMCH	TCMH	2020-01-22 21:46:11.21754-08
39	test name	CMCH	Concentration Corpusculaire Moyenne en Hémoglobine	2020-01-22 21:46:11.21754-08
40	test report name	CMCH	CCMH	2020-01-22 21:46:11.21754-08
41	test name	Platelets	Plaquette	2020-01-22 21:46:11.21754-08
42	test report name	Platelets	Plaquettes	2020-01-22 21:46:11.21754-08
43	test name	Neutrophiles (%)	Polynucléaires Neutrophiles (%)	2020-01-22 21:46:11.21754-08
44	test report name	Neutrophiles (%)	PNN %	2020-01-22 21:46:11.21754-08
45	test name	Neutrophiles	Polynucléaires Neutrophiles (Abs)	2020-01-22 21:46:11.21754-08
46	test report name	Neutrophiles	PNN (Abs)	2020-01-22 21:46:11.21754-08
47	test name	Eosinophiles (%)	Polynucléaires Eosinophiles (%)	2020-01-22 21:46:11.21754-08
48	test report name	Eosinophiles (%)	PNE (%)	2020-01-22 21:46:11.21754-08
49	test name	Eosinophiles	Polynucléaires Eosinophiles (Abs)	2020-01-22 21:46:11.21754-08
50	test report name	Eosinophiles	PNE (Abs)	2020-01-22 21:46:11.21754-08
51	test name	Basophiles (%)	Polynucléaires basophiles (%)	2020-01-22 21:46:11.21754-08
52	test report name	Basophiles (%)	PNB (%)	2020-01-22 21:46:11.21754-08
53	test name	Basophiles	Polynucléaires basophiles (Abs)	2020-01-22 21:46:11.21754-08
54	test report name	Basophiles	PNB (Abs)	2020-01-22 21:46:11.21754-08
55	test name	Lymphocytes (%)	Lymphocytes (%)	2020-01-22 21:46:11.21754-08
56	test report name	Lymphocytes (%)	Lympho (%)	2020-01-22 21:46:11.21754-08
57	test name	Lymphocytes (Abs)	Lymphocytes (Abs)	2020-01-22 21:46:11.21754-08
58	test report name	Lymphocytes (Abs)	Lympho (Abs)	2020-01-22 21:46:11.21754-08
59	test name	Monocytes (%)	Monocytes (%)	2020-01-22 21:46:11.21754-08
60	test report name	Monocytes (%)	Mono (%)	2020-01-22 21:46:11.21754-08
61	test name	Monocytes (Abs)	Monocytes (Abs)	2020-01-22 21:46:11.21754-08
62	test report name	Monocytes (Abs)	Mono (Abs)	2020-01-22 21:46:11.21754-08
69	test name	CD4 Absolute count (mm3)	Dénombrement des lymphocytes CD4 (mm3)	2020-01-22 21:46:11.21754-08
70	test report name	CD4 Absolute count (mm3)	CD4  (Abs)	2020-01-22 21:46:11.21754-08
71	test name	CD4 percent  (%)	Dénombrement des lymphocytes  CD4 (%)	2020-01-22 21:46:11.21754-08
72	test report name	CD4 percent  (%)	CD4 %	2020-01-22 21:46:11.21754-08
73	test name	HBsAg (Hepatitis B surface antigen)	HBs AG (antigén australia)	2020-01-22 21:46:11.21754-08
74	test report name	HBsAg (Hepatitis B surface antigen)	Ag HBs	2020-01-22 21:46:11.21754-08
75	test name	Viral load	Mesure de la charge virale	2020-01-22 21:46:11.21754-08
76	test report name	Viral load	Charge virale VIH	2020-01-22 21:46:11.21754-08
77	test name	ARV resistance	Detection de la resistance aux antiretroviraux	2020-01-22 21:46:11.21754-08
78	test report name	ARV resistance	ARV res	2020-01-22 21:46:11.21754-08
79	test name	Western blot HIV	Western blot VIH	2020-01-22 21:46:11.21754-08
80	test report name	Western blot HIV	WB-Serum	2020-01-22 21:46:11.21754-08
81	test name	Western blot HIV	Western blot VIH	2020-01-22 21:46:11.21754-08
82	test report name	Western blot HIV	WB-Plasma	2020-01-22 21:46:11.21754-08
83	test name	Bioline	Bioline	2020-01-22 21:46:11.21754-08
84	test report name	Bioline	Bioline-Plasma	2020-01-22 21:46:11.21754-08
85	test name	Bioline	Bioline	2020-01-22 21:46:11.21754-08
86	test report name	Bioline	Bioline-Serum	2020-01-22 21:46:11.21754-08
87	test name	Bioline	Bioline	2020-01-22 21:46:11.21754-08
88	test report name	Bioline	Bioline-Sang total	2020-01-22 21:46:11.21754-08
89	test name	Genie III	Genie III	2020-01-22 21:46:11.21754-08
33	test name	Hematocrit	Hématocrite	2020-01-22 21:46:11.21754-08
34	test report name	Hematocrit	Hématocrite	2020-01-22 21:46:11.21754-08
90	test report name	Genie III	Genie III-Plasma	2020-01-22 21:46:11.21754-08
91	test name	Genie III	Genie III	2020-01-22 21:46:11.21754-08
92	test report name	Genie III	Genie III-Serum	2020-01-22 21:46:11.21754-08
93	test name	Genie III	Genie III	2020-01-22 21:46:11.21754-08
94	test report name	Genie III	Genie III-Sang t	2020-01-22 21:46:11.21754-08
95	test name	Murex	Murex	2020-01-22 21:46:11.21754-08
96	test report name	Murex	Murex-Plasma	2020-01-22 21:46:11.21754-08
97	test name	Murex	Murex	2020-01-22 21:46:11.21754-08
98	test report name	Murex	Murex-Serum	2020-01-22 21:46:11.21754-08
99	test name	Vironostika	Vironostika	2020-01-22 21:46:11.21754-08
100	test report name	Vironostika	Vironostika-Plas	2020-01-22 21:46:11.21754-08
101	test name	Vironostika	Vironostika	2020-01-22 21:46:11.21754-08
102	test report name	Vironostika	Vironostika-Seru	2020-01-22 21:46:11.21754-08
103	test name	P24 Ag	P24 Ag	2020-01-22 21:46:11.21754-08
104	test report name	P24 Ag	P24 Ag-Plasma	2020-01-22 21:46:11.21754-08
105	test name	P24 Ag	P24 Ag	2020-01-22 21:46:11.21754-08
106	test report name	P24 Ag	P24 Ag-Serum	2020-01-22 21:46:11.21754-08
107	test name	Stat-Pak	Stat-Pak	2020-01-22 21:46:11.21754-08
108	test report name	Stat-Pak	Stat-Pak-Plasma	2020-01-22 21:46:11.21754-08
109	test name	Stat-Pak	Stat-Pak	2020-01-22 21:46:11.21754-08
110	test report name	Stat-Pak	Stat-Pak-Sérum	2020-01-22 21:46:11.21754-08
111	test name	Stat-Pak	Stat-Pak	2020-01-22 21:46:11.21754-08
112	test report name	Stat-Pak	Stat-Pak-Sang total	2020-01-22 21:46:11.21754-08
113	test name	Determine	Determine	2020-01-22 21:46:11.21754-08
114	test report name	Determine	Determine-Plasma	2020-01-22 21:46:11.21754-08
115	test name	Determine	Determine	2020-01-22 21:46:11.21754-08
116	test report name	Murex	Determine-Sérum	2020-01-22 21:46:11.21754-08
117	test name	Vironostika	Determine	2020-01-22 21:46:11.21754-08
118	test report name	Vironostika	Determine-Sang total	2020-01-22 21:46:11.21754-08
63	test name	HIV rapid test HIV	Test rapide VIH	2020-01-22 21:46:11.353783-08
65	test name	HIV rapid test HIV	Test rapide VIH	2020-01-22 21:46:11.353783-08
67	test name	HIV rapid test HIV	Test rapide VIH	2020-01-22 21:46:11.353783-08
64	test report name	HIV rapid test HIV	VIH rapide-Serum	2020-01-22 21:46:11.353783-08
66	test report name	HIV rapid test HIV	VIH rapide-Plasm	2020-01-22 21:46:11.353783-08
68	test report name	HIV rapid test HIV	VIH rapide-SangT	2020-01-22 21:46:11.353783-08
119	test unit name	Bacteria	Bactériologie	2020-01-22 21:46:36.196132-08
120	test unit name	Parasitology	Parasitologie	2020-01-22 21:46:36.196132-08
121	test unit name	ECBU	ECBU	2020-01-22 21:46:36.196132-08
122	test unit name	VCT	VCT	2020-01-22 21:46:36.196132-08
123	test unit name	Virology	Virologie	2020-01-22 21:46:36.196132-08
124	test unit name	Mycobacteriology	Mycobactériologie	2020-01-22 21:46:36.196132-08
125	test unit name	user	user	2020-01-22 21:46:36.196132-08
126	test unit name	Hemato-Immunology	Hémato-Immunologie	2020-01-22 21:46:36.196132-08
127	test unit name	Biochemistry	Biochimie	2020-01-22 21:46:36.196132-08
128	test unit name	Hematology	Hématologie	2020-01-22 21:46:36.196132-08
129	test unit name	Serology-Immunology	Sérologie-Immunologie	2020-01-22 21:46:36.196132-08
130	test unit name	Immunology	Immunologie	2020-01-22 21:46:36.196132-08
131	test unit name	Molecular Biology	Biologie Moleculaire	2020-01-22 21:46:36.196132-08
132	panel name	Bilan Biochimique	Bilan Biochimique	2020-01-22 21:46:36.230762-08
133	panel name	NFS	NFS	2020-01-22 21:46:36.230762-08
134	panel name	Serologie VIH	Serologie VIH	2020-01-22 21:46:36.230762-08
135	panel name	Typage lymphocytaire	Typage lymphocytaire	2020-01-22 21:46:36.230762-08
136	sampleType name	Variable	Varié	2020-01-22 21:46:36.340022-08
137	sampleType name	Plasma	Plasma	2020-01-22 21:46:36.340022-08
138	sampleType name	Whole Blood	Whole Blood	2020-01-22 21:46:36.340022-08
139	sampleType name	Serum	Sérum	2020-01-22 21:46:36.340022-08
140	sampleType name	Urines	Urines	2020-01-22 21:46:36.340022-08
141	sampleType name	Dry Tube	Tube sec - Rouge	2020-01-22 21:46:41.326636-08
142	sampleType name	EDTA Tube	Tube EDTA - Violet	2020-01-22 21:46:41.326636-08
143	sampleType name	DBS	DBS	2020-01-22 21:46:41.326636-08
400	test unit name	EID	EID	2020-01-22 21:46:41.338786-08
401	test unit name	VL	VL	2020-01-22 21:46:41.338786-08
200	test name	Glycémie	Glycémie	2020-01-22 21:46:41.911875-08
201	test report name	Glycémie	Glycémie	2020-01-22 21:46:41.911875-08
202	test name	Créatininémie	Créatininémie	2020-01-22 21:46:41.911875-08
203	test report name	Créatininémie	Créatininémie	2020-01-22 21:46:41.911875-08
204	test name	Transaminases ALTL	Transaminases ALTL	2020-01-22 21:46:41.911875-08
205	test report name	Transaminases ALTL	Transaminases ALTL	2020-01-22 21:46:41.911875-08
206	test name	p24 Ag	p24 Ag	2020-01-22 21:46:41.911875-08
207	test report name	p24 Ag	p24 Ag	2020-01-22 21:46:41.911875-08
208	test name	Western Blot 2	Western Blot 2	2020-01-22 21:46:41.911875-08
209	test report name	Western Blot 2	Western Blot 2	2020-01-22 21:46:41.911875-08
210	test name	Western Blot 1	Western Blot 1	2020-01-22 21:46:41.911875-08
211	test report name	Western Blot 1	Western Blot 1	2020-01-22 21:46:41.911875-08
212	test name	Genie II 10	Genie II 10	2020-01-22 21:46:41.911875-08
213	test report name	Genie II 10	Genie II 10	2020-01-22 21:46:41.911875-08
214	test name	Genie II 100	Genie II 100	2020-01-22 21:46:41.911875-08
215	test report name	Genie II 100	Genie II 100	2020-01-22 21:46:41.911875-08
216	test name	Genie II	Genie II	2020-01-22 21:46:41.911875-08
217	test report name	Genie II	Genie II	2020-01-22 21:46:41.911875-08
218	test name	Vironostika	Vironostika	2020-01-22 21:46:41.911875-08
219	test report name	Vironostika	Vironostika	2020-01-22 21:46:41.911875-08
220	test name	Murex	Murex	2020-01-22 21:46:41.911875-08
221	test report name	Murex	Murex	2020-01-22 21:46:41.911875-08
222	test name	Integral	Integral	2020-01-22 21:46:41.911875-08
223	test report name	Integral	Integral	2020-01-22 21:46:41.911875-08
224	test name	CD4	CD4	2020-01-22 21:46:41.911875-08
225	test report name	CD4	CD4	2020-01-22 21:46:41.911875-08
226	test name	Génotypage	Génotypage	2020-01-22 21:46:41.911875-08
227	test report name	Génotypage	Génotypage	2020-01-22 21:46:41.911875-08
228	test name	Viral Load	Viral Load	2020-01-22 21:46:41.911875-08
229	test report name	Viral Load	Viral Load	2020-01-22 21:46:41.911875-08
230	test name	DNA PCR	DNA PCR	2020-01-22 21:46:41.911875-08
231	test report name	DNA PCR	DNA PCR	2020-01-22 21:46:41.911875-08
232	test name	Transaminases ASTL	Transaminases ASTL	2020-01-22 21:46:41.911875-08
233	test report name	Transaminases ASTL	Transaminases ASTL	2020-01-22 21:46:41.911875-08
234	test name	GB	GB	2020-01-22 21:46:41.911875-08
235	test report name	GB	GB	2020-01-22 21:46:41.911875-08
236	test name	PLQ	PLQ	2020-01-22 21:46:41.911875-08
237	test report name	PLQ	PLQ	2020-01-22 21:46:41.911875-08
238	test name	Neut %	Neut %	2020-01-22 21:46:41.911875-08
239	test report name	Neut %	Neut %	2020-01-22 21:46:41.911875-08
240	test name	Lymph %	Lymph %	2020-01-22 21:46:41.911875-08
241	test report name	Lymph %	Lymph %	2020-01-22 21:46:41.911875-08
242	test name	Mono %	Mono %	2020-01-22 21:46:41.911875-08
243	test report name	Mono %	Mono %	2020-01-22 21:46:41.911875-08
244	test name	Eo %	Eo %	2020-01-22 21:46:41.911875-08
245	test report name	Eo %	Eo %	2020-01-22 21:46:41.911875-08
246	test name	Baso %	Baso %	2020-01-22 21:46:41.911875-08
247	test report name	Baso %	Baso %	2020-01-22 21:46:41.911875-08
248	test name	HCT	HCT	2020-01-22 21:46:41.911875-08
249	test report name	HCT	HCT	2020-01-22 21:46:41.911875-08
250	test name	CCMH	CCMH	2020-01-22 21:46:41.911875-08
251	test report name	CCMH	CCMH	2020-01-22 21:46:41.911875-08
252	test name	GR	GR	2020-01-22 21:46:41.911875-08
253	test report name	GR	GR	2020-01-22 21:46:41.911875-08
254	test name	Hb	Hb	2020-01-22 21:46:41.911875-08
255	test report name	Hb	Hb	2020-01-22 21:46:41.911875-08
256	test name	VGM	VGM	2020-01-22 21:46:41.911875-08
257	test report name	VGM	VGM	2020-01-22 21:46:41.911875-08
258	test name	TCMH	TCMH	2020-01-22 21:46:41.911875-08
259	test report name	TCMH	TCMH	2020-01-22 21:46:41.911875-08
260	test name	CD3 percentage count	CD3 percentage count	2020-01-22 21:46:41.911875-08
261	test report name	CD3 percentage count	CD3 percentage count	2020-01-22 21:46:41.911875-08
262	test name	CD4 percentage count	CD4 percentage count	2020-01-22 21:46:41.911875-08
263	test report name	CD4 percentage count	CD4 percentage count	2020-01-22 21:46:41.911875-08
264	test name	CD4 absolute count	CD4 absolute count	2020-01-22 21:46:41.911875-08
265	test report name	CD4 absolute count	CD4 absolute count	2020-01-22 21:46:41.911875-08
266	test name	NE#	NE#	2020-01-22 21:46:41.911875-08
267	test report name	NE#	NE#	2020-01-22 21:46:41.911875-08
268	test name	MO#	MO#	2020-01-22 21:46:41.911875-08
269	test report name	MO#	MO#	2020-01-22 21:46:41.911875-08
270	test name	BA#	BA#	2020-01-22 21:46:41.911875-08
271	test report name	BA#	BA#	2020-01-22 21:46:41.911875-08
272	test name	LY#	LY#	2020-01-22 21:46:41.911875-08
273	test report name	LY#	LY#	2020-01-22 21:46:41.911875-08
274	test name	EO#	EO#	2020-01-22 21:46:41.911875-08
275	test report name	EO#	EO#	2020-01-22 21:46:41.911875-08
276	test name	Bioline	Bioline	2020-01-22 21:46:41.911875-08
277	test report name	Bioline	Bioline	2020-01-22 21:46:41.911875-08
278	test name	Innolia	Innolia	2020-01-22 21:46:41.911875-08
279	test report name	Innolia	Innolia	2020-01-22 21:46:41.911875-08
280	test unit name	Serology	Sérologie	2020-01-22 21:46:41.941042-08
\.


--
-- Name: localization_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.localization_seq', 401, true);


--
-- Data for Name: login_user; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.login_user (id, login_name, password, password_expired_dt, account_locked, account_disabled, is_admin, user_time_out) FROM stdin;
\.


--
-- Name: login_user_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.login_user_seq', 135, true);


--
-- Data for Name: menu; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.menu (id, parent_id, presentation_order, element_id, action_url, click_action, display_key, tool_tip_key, new_window, is_active) FROM stdin;
1	\N	1	menu_home	/Dashboard.do	\N	banner.menu.home	tooltip.bannner.menu.home	f	t
2	\N	2	menu_sample	\N	\N	banner.menu.sample	banner.menu.sample	f	t
3	2	1	menu_sample_add	/SamplePatientEntry.do	\N	banner.menu.sampleAdd	banner.menu.sampleAdd	f	t
4	\N	3	menu_patient	\N	\N	banner.menu.patient	tooltip.bannner.menu.patient	f	t
5	4	1	menu_patient_add_or_edit	/PatientManagement.do	\N	banner.menu.patient.addOrEdit	tooltip.banner.menu.patient.addOrEdit	f	t
7	\N	5	menu_workplan	\N	\N	banner.menu.workplan	tooltip.bannner.menu.workplan	f	t
8	7	1	menu_workplan_test	/WorkPlanByTest.do?type=test	\N	banner.menu.workplan.test	tooltip.bannner.menu.workplan.test	f	t
9	7	2	menu_workplan_panel	/WorkPlanByPanel.do?type=panel	\N	banner.menu.workplan.panel	tooltip.bannner.menu.workplan.panel	f	t
10	7	3	menu_workplan_bench	/WorkPlanByTestSection.do?type=	\N	banner.menu.workplan.bench	tooltip.bannner.menu.workplan.bench	f	t
11	\N	6	menu_results	\N	\N	banner.menu.results	tooltip.bannner.menu.results	f	t
12	11	1	menu_results_logbook	/LogbookResults.do?type=	\N	banner.menu.results.logbook	banner.menu.results.logbook	f	t
13	11	2	menu_results_search	\N	\N	banner.menu.results.search	banner.menu.results.search	f	t
14	13	1	menu_results_patient	/PatientResults.do	\N	banner.menu.results.patient	banner.menu.results.patient	f	t
15	13	2	menu_results_accession	/AccessionResults.do	\N	banner.menu.results.accession	banner.menu.results.accession	f	t
16	13	3	menu_results_status	/StatusResults.do?blank=true	\N	banner.menu.results.status	banner.menu.results.status	f	t
17	\N	7	menu_resultvalidation	/ResultValidation.do?type=&test=	\N	banner.menu.resultvalidation	tooltip.banner.menu.resultvalidation	f	t
18	\N	9	menu_reports	\N	\N	banner.menu.reports	tooltip.banner.menu.reports	f	t
6	\N	4	menu_nonconformity	\N	\N	banner.menu.nonconformity	tooltip.banner.menu.nonconformity	f	t
180	6	1	menu_non_conforming_report	/ReportNonConformingEvent.do	\N	banner.menu.nonconformity.report	tooltip.banner.menu.nonconformity.report	f	t
181	6	2	menu_non_conforming_view	/ViewNonConformingEvent.do	\N	banner.menu.nonconformity.view	tooltip.banner.menu.nonconformity.view	f	t
22	\N	10	menu_administration	/MasterListsPage.do	\N	banner.menu.administration	tooltip.banner.menu.administration	f	t
23	22	1	menu_administration_test_management	/TestManagementConfigMenu.do	\N	configuration.test.management	configuration.test.management	f	t
182	6	3	menu_non_conforming_corrective_actions	/NCECorrectiveAction.do	\N	banner.menu.nonconformity.correctiveActions	tooltip.banner.menu.nonconformity.correctiveActions	f	t
183	22	2	menu_administration_report_management	/ReportConfiguration.do	\N	banner.menu.reportManagement	tooltip.banner.reportManagement	f	t
30	2	4	menu_sample_edit	/SampleEdit.do?type=readwrite	\N	banner.menu.sampleEdit	banner.menu.sampleEdit	f	t
31	11	3	menu_results_referred	/ReferredOutTests.do	\N	banner.menu.referredOut	tooltip.banner.menu.referredOut	f	t
40	2	2	menu_sample_create	\N	\N	banner.menu.sampleCreate	tooltip.bannner.menu.sampleCreate	f	t
41	40	1	menu_sample_create_initial	/SampleEntryByProject.do?type=initial	\N	banner.menu.createInitial	tooltip.bannner.menu.createInitial	f	t
42	40	2	menu_sample_create_double	/SampleEntryByProject.do?type=verify	\N	banner.menu.createDouble	tooltip.banner.menu.createDouble	f	t
43	40	3	menu_sample_consult	/SampleEdit.do?type=readonly	\N	banner.menu.sampleConsult	tooltip.banner.menu.sampleConsult	f	t
44	4	2	menu_patient_create	\N	\N	banner.menu.patient.Create	tooltip.banner.menu.patient.Create	f	t
45	44	1	menu_patient_create_initial	/PatientEntryByProject.do?type=initial	\N	banner.menu.createInitial	tooltip.bannner.menu.createInitial	f	t
46	44	2	menu_patient_create_double	/PatientEntryByProject.do?type=verify	\N	banner.menu.createDouble	tooltip.banner.menu.createDouble	f	t
47	44	3	menu_patient_edit	/PatientEditByProject.do?type=readwrite	\N	banner.menu.patientEdit	tooltip.banner.menu.patientEdit	f	t
48	44	4	menu_patient_consult	/PatientEditByProject.do?type=readonly	\N	banner.menu.patientConsult	tooltip.banner.menu.patientConsult	f	t
49	17	1	menu_resultvalidation_routine	/ResultValidation.do?type=&test=	\N	banner.menu.resultvalidation_routine	tooltip.bannner.menu.resultvalidation_routine	f	t
50	17	2	menu_resultvalidation_study	\N	\N	banner.menu.resultvalidation_study	tooltip.bannner.menu.resultvalidation_study	f	t
26	20	10	menu_reports_activity	\N	\N	openreports.activity.title	openreports.activity.title	f	t
38	\N	11	menu_help	\N	\N	banner.menu.help	tooltip.banner.menu.help	t	t
53	50	3	menu_resultvalidation_serology	/ResultValidationRetroC.do?type=serology	\N	banner.menu.resultvalidation.serology	tooltip.banner.menu.resultvalidation.biochemistry	f	t
55	54	1	menu_resultvalidation_dnapcr	/ResultValidationRetroC.do?type=virology&test=DNA PCR	\N	banner.menu.resultvalidation.dnapcr	tooltip.banner.menu.resultvalidation.dnapcr	f	t
56	54	2	menu_resultvalidation_viralload	/ResultValidationRetroC.do?type=virology&test=Viral Load	\N	banner.menu.resultvalidation.viralload	banner.menu.resultvalidation.viralload	f	t
57	54	3	menu_resultvalidation_genotyping	/ResultValidationRetroC.do?type=virology&test=Genotyping	\N	banner.menu.resultvalidation.genotyping	tooltip.banner.menu.resultvalidation.genotyping	f	t
58	18	1	menu_reports_routine	\N	\N	banner.menu.reports.routine	tooltip.banner.menu.reports.routine	f	t
59	58	50	menu_reports_export_routine	/Report.do?type=patient&report=CISampleRoutineExport	\N	reports.export.byDate_routine	tooltip.reports.export.byDate_routine	f	t
60	32	20	menu_reports_status_patient.vreduit	/Report.do?type=patient&report=patientCILNSP_vreduit	\N	openreports.patientTestStatus.vreduit	tooltip.openreports.patientTestStatus.vreduit	f	t
61	32	10	menu_reports_status_patient.classique	/Report.do?type=patient&report=patientCILNSP	\N	openreports.patientTestStatus.classique	tooltip.openreports.patientTestStatus.classique	f	t
19	58	2	menu_reports_aggregate	\N	\N	openreports.aggregate.title	tooltip.openreports.aggregate.title	f	t
20	58	3	menu_reports_management	\N	\N	openreports.management.title	openreports.management.title	f	t
32	58	1	menu_reports_status_patient	/Report.do?type=patient&report=patientCILNSP	\N	openreports.patientTestStatus	tooltip.openreports.patientTestStatus	f	t
21	20	50	menu_reports_nonconformity	\N	\N	reports.nonConformity.menu	tooltip.reports.nonConformity.menu	f	t
24	20	60	menu_reports_validation_backlog	/ReportPrint.do?type=indicator&report=validationBacklog	\N	banner.menu.report.validation.backlog	banner.menu.report.validation.backlog	t	t
25	20	70	menu_reports_auditTrail	/AuditTrailReport.do	\N	reports.auditTrail	reports.auditTrail	f	t
37	20	20	menu_reports_referred	/Report.do?type=patient&report=referredOut	\N	reports.label.referred.out	tooltip.reports.label.referred.out	f	t
27	26	1	menu_activity_report_test	/Report.do?type=indicator&report=activityReportByTest	\N	banner.menu.workplan.test	tooltip.bannner.menu.workplan.test	f	t
28	26	2	menu_activity_report_panel	/Report.do?type=indicator&report=activityReportByPanel	\N	banner.menu.workplan.panel	tooltip.bannner.menu.workplan.panel	f	t
29	26	3	menu_activity_report_bench	/Report.do?type=indicator&report=activityReportByTestSection	\N	banner.menu.workplan.bench	tooltip.bannner.menu.workplan.bench	f	t
35	21	1	menu_reports_nonconformity_date	/Report.do?type=patient&report=haitiNonConformityByDate	\N	reports.nonConformity.byDate.report	tooltip.reports.nonConformity.byDate.report	f	t
36	21	2	menu_reports_nonconformity_section	/Report.do?type=patient&report=haitiNonConformityBySectionReason	\N	reports.nonConformity.bySectionReason.report	tooltip.reports.nonConformity.bySectionReason.report	f	t
33	19	20	menu_reports_aggregate_hiv	/Report.do?type=indicator&report=indicatorCDILNSPHIV	\N	openreports.hiv.aggregate	tooltip.openreports.hiv.aggregate	f	t
34	19	10	menu_reports_aggregate_all	/Report.do?type=indicator&report=indicatorHaitiLNSPAllTests	\N	openreports.all.tests.aggregate	tooltip.openreports.all.tests.aggregate	f	t
62	18	2	menu_reports_study	\N	\N	banner.menu.reports.study	tooltip.banner.menu.reports.study	f	t
63	62	10	menu_reports_patients	\N	\N	openreports.patientreports.title	tooltip.openreports.patientreports.title	f	t
64	63	1	menu_reports_arv	\N	\N	project.ARVStudies.name	project.ARVStudies.name	f	t
65	64	1	menu_reports_arv_initial1	/Report.do?type=patient&report=patientARVInitial1	\N	reports.patient.ARVInitial.version1	tooltip.reports.patient.ARVInitial.version1	f	t
66	64	2	menu_reports_arv_initial2	/Report.do?type=patient&report=patientARVInitial2	\N	reports.patient.ARVInitial.version2	tooltip.reports.patient.ARVInitial.version2	f	t
67	64	3	menu_reports_arv_followup1	/Report.do?type=patient&report=patientARVFollowup1	\N	reports.patient.ARVFollowup.version1	tooltip.reports.patient.ARVFollowup.version1	f	t
68	64	4	menu_reports_arv_followup2	/Report.do?type=patient&report=patientARVFollowup2	\N	reports.patient.ARVFollowup.version2	tooltip.reports.patient.ARVFollowup.version2	f	t
69	64	5	menu_reports_arv_all	/Report.do?type=patient&report=patientARV1	\N	reports.patient.ARV.version1	tooltip.reports.patient.ARVFollowup.version1	f	t
70	63	2	menu_reports_eid	\N	\N	project.EIDStudy.name	tooltip.project.EIDStudy.name	f	t
71	70	1	menu_reports_eid_version1	/Report.do?type=patient&report=patientEID1	\N	openreports.patient.EID.version1	tooltip.openreports.patient.EID.version1	f	t
72	70	2	menu_reports_eid_version2	/Report.do?type=patient&report=patientEID2	\N	openreports.patient.EID.version2	tooltip.openreports.patient.EID.version2	f	t
73	63	30	menu_reports_indeterminate	\N	\N	project.IndeterminateStudy.name	tooltip.project.IndeterminateStudy.name	f	t
74	73	1	menu_reports_indeterminate_version1	/Report.do?type=patient&report=patientIndeterminate1	\N	reports.patient.Indeterminate.version1	tooltip.reports.patient.Indeterminate.version1	f	t
75	73	2	menu_reports_indeterminate_version2	/Report.do?type=patient&report=patientIndeterminate2	\N	reports.patient.Indeterminate.version2	tooltip.reports.patient.Indeterminate.version2	f	t
76	73	3	menu_reports_indeterminate_location	/Report.do?type=patient&report=patientIndeterminateByLocation	\N	reports.patient.Indeterminate.location	tooltip.reports.patient.Indeterminate.location	f	t
51	50	1	menu_resultvalidation_immunology	/ResultValidationRetroC.do?type=Immunology&test=	\N	banner.menu.resultvalidation.immunologyHematology	tooltip.banner.menu.resultvalidation.immunologyHematology	f	t
77	63	40	menu_reports_special	/Report.do?type=patient&report=patientSpecialReport	\N	project.SpecialRequestStudy.name	tooltip.project.SpecialRequestStudy.name	f	t
78	63	50	menu_reports_patient_collection	/Report.do?type=patient&report=patientCollection	\N	patient.report.collection.name	patient.report.collection.name	f	t
79	63	60	menu_reports_patient_associated	/Report.do?type=patient&report=patientAssociated	\N	patient.report.associated.name	patient.report.associated.name	f	t
80	62	20	menu_reports_indicator	\N	\N	reports.label.indicator	reports.label.indicator	f	t
81	80	1	menu_reports_indicator_performance	/ReportPrint.do?type=indicator&report=indicatorSectionPerformance	\N	reports.label.indicator.performance	tooltip.reports.label.indicator.performance	f	t
82	80	2	menu_reports_validation_backlog.study	/ReportPrint.do?type=indicator&report=validationBacklog	\N	banner.menu.report.validation.backlog	banner.menu.report.validation.backlog	f	t
83	62	30	menu_reports_nonconformity.study	\N	\N	reports.nonConformity.menu	tooltip.reports.nonConformity.menu	f	t
84	83	1	menu_reports_nonconformity_date.study	/Report.do?type=patient&report=retroCINonConformityByDate	\N	reports.nonConformity.byDate.report	tooltip.reports.nonConformity.byDate.report	f	t
85	83	2	menu_reports_nonconformity_section.study	/Report.do?type=patient&report=retroCInonConformityBySectionReason	\N	reports.nonConformity.bySectionReason.report	tooltip.reports.nonConformity.bySectionReason.report	f	t
86	83	4	menu_reports_nonconformity_notification.study	/Report.do?type=patient&report=retroCInonConformityNotification	\N	reports.nonConformity.notification.report	tooltip.reports.nonConformity.notification.report	f	t
87	83	5	menu_reports_followupRequired_ByLocation.study	/Report.do?type=patient&report=retroCIFollowupRequiredByLocation	\N	reports.followupRequired.byLocation	tooltip.reports.followupRequired.byLocation	f	t
88	83	3	menu_reports_nonconformity.Labno	/Report.do?type=patient&report=retroCINonConformityByLabno	\N	reports.nonConformity.byLabno.report	tooltip.reports.nonConformity.byLabno.report	f	t
90	62	50	menu_reports_auditTrail.study	/AuditTrailReport.do	\N	reports.auditTrail	reports.auditTrail	f	t
91	63	3	menu_reports_vl			project.VLStudy.name	tooltip.project.VLStudy.name	f	t
92	91	1	menu_reports_vl_version1	/Report.do?type=patient&report=patientVL1		openreports.patient.VL.version1	tooltip.openreports.patient.VL.version1	f	t
52	50	2	menu_resultvalidation_biochemistry	/ResultValidationRetroC.do?type=Biochemistry&test=	\N	banner.menu.resultvalidation.biochemistry	tooltip.banner.menu.resultvalidation.immunologyHematology	f	t
54	50	4	menu_resultvalidation_virology	\N	\N	banner.menu.resultvalidation.virology	tooltip.banner.menu.resultvalidation.virology	f	t
170	2	9	menu_sample_batch_entry	/SampleBatchEntrySetup.do	\N	banner.menu.sampleBatchEntry	banner.menu.sampleBatchEntry	f	t
171	2	10	menu_sample_print_barcode	/PrintBarcode.do	\N	banner.menu.printBarcode	banner.menu.printBarcode	f	t
89	62	40	menu_reports_export	\N	\N	reports.export.byDate	tooltip.reports.export.byDate	f	t
174	2	5	menu_sample_eorder	/ElectronicOrders.do	\N	banner.menu.eorders	tooltip.bannner.menu.eorders	f	t
175	38	1	menu_help_user_manual	/documentation/UserManual	\N	banner.menu.help.usermanual	tooltip.bannner.menu.help.usermanual	t	t
176	38	2	menu_help_documents	\N	\N	banner.menu.help.documents	tooltip.bannner.menu.help.documents	f	t
177	176	1	menu_help_form_VL	/documentation/FICHE_DEMANDE_CHARGE_VIRALE_VF_25102016.pdf	\N	banner.menu.help.formVL	tooltip.bannner.menu.help.formVL	t	t
178	176	2	menu_help_form_DBS	/documentation/DBS_Identn_18Juin2010.pdf	\N	banner.menu.help.formDBS	tooltip.bannner.menu.help.formDBS	t	t
173	89	30	menu_reports_export_specific	/Report.do?type=patient&report=Trends	\N	reports.export.specific	tooltip.export.cpecific	f	t
172	89	10	menu_reports_export_general	/Report.do?type=patient&report=CIStudyExport	\N	reports.export.general	tooltip.export.generale	f	t
179	89	20	menu_reports_export_valid	/Report.do?type=patient&report=CISampleExport	\N	reports.export.valid	tooltip.export.valid	f	t
\.


--
-- Name: menu_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.menu_seq', 183, true);


--
-- Data for Name: message_org; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.message_org (id, org_id, is_active, active_begin, active_end, description, lastupdated) FROM stdin;
25	1956	Y	2007-03-07 00:00:00	\N	COLORADO	2007-03-07 15:21:05.819
21	2133	Y	2007-03-07 00:00:00	\N	CDC	2007-03-07 15:19:35.712
22	2294	Y	2007-03-07 00:00:00	\N	IOWA	2007-03-07 15:19:56.455
23	2015	Y	2007-03-07 00:00:00	\N	CALIFORNIA	2007-03-07 15:25:42.987
24	2296	Y	2007-03-07 00:00:00	\N	NEBRASKA	2007-03-07 15:20:35.645
\.


--
-- Name: message_org_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.message_org_seq', 41, false);


--
-- Name: messagecontrolidseq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.messagecontrolidseq', 1, false);


--
-- Data for Name: method; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.method (id, name, description, reporting_description, is_active, active_begin, active_end, lastupdated) FROM stdin;
1	EIA 	Enzyme-linked immunoassay	EIA 	Y	2007-04-24 00:00:00	\N	2007-04-24 13:46:47.063
31	PCR	Polymerase Chain Reaction	\N	Y	2006-09-18 00:00:00	\N	2006-09-29 14:32:51
32	STAIN	Stain	\N	Y	2006-09-29 00:00:00	\N	2006-09-29 14:32:57
33	CULTURE	Culture	\N	Y	2006-09-29 00:00:00	\N	2006-11-01 08:10:49.606
34	PROBE	Probe	\N	Y	2006-09-29 00:00:00	\N	2006-09-29 14:33:05
35	BIOCHEMICAL	Biochemical	\N	Y	2006-09-29 00:00:00	\N	2006-11-08 09:16:24.377
27	Diane Test	Diane Test	\N	Y	2006-09-06 00:00:00	\N	2006-10-23 15:35:39.534
36	HPLC	High Pressure Liquid Chromatography	\N	Y	2006-09-29 00:00:00	\N	2006-09-29 14:31:50
37	DNA SEQUENCING	DNA Sequencing	\N	Y	2006-09-29 00:00:00	\N	2006-10-23 15:35:40.691
3	AUTO	Automated (Haiti)		Y	2009-02-24 00:00:00	\N	2009-02-24 16:26:17.507
4	MANUAL	test done manually (Haiti)		Y	2009-02-24 00:00:00	\N	2009-02-24 16:26:47.604
5	HIV_TEST_KIT	Uses Hiv test kit		Y	2009-03-05 00:00:00	\N	2009-03-05 14:26:19.46
6	SYPHILIS_TEST_KIT	Test kit for syphilis		Y	2009-03-05 00:00:00	\N	2009-03-05 14:28:11.61
\.


--
-- Data for Name: method_analyte; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.method_analyte (id, method_id, analyte_id, result_group, sort_order, ma_type) FROM stdin;
\.


--
-- Data for Name: method_result; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.method_result (id, scrip_id, result_group, flags, methres_type, value, quant_limit, cont_level, method_id) FROM stdin;
\.


--
-- Name: method_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.method_seq', 6, true);


--
-- Data for Name: mls_lab_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.mls_lab_type (id, description, org_mlt_org_mlt_id) FROM stdin;
\.


--
-- Data for Name: nc_event; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.nc_event (id, name, name_of_reporter, report_date, nce_number, date_of_event, lab_order_number, prescriber_name, site, reporting_unit_id, description, suspected_causes, proposed_action, laboratory_component, nce_category_id, consequence_id, recurrence_id, severity_score, color_code, corrective_action, control_action, comments, effective, signature, date_completed, nce_type_id, status, discussion_date) FROM stdin;
\.


--
-- Name: nc_event_id_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.nc_event_id_seq', 1, false);


--
-- Data for Name: nce_action_log; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.nce_action_log (id, corrective_action, action_type, date_completed, turn_around_time, nce_event_id, person_responsible) FROM stdin;
\.


--
-- Name: nce_action_log_id_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.nce_action_log_id_seq', 1, false);


--
-- Data for Name: nce_category; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.nce_category (id, name, display_key, active, last_updated) FROM stdin;
1	General	nce.category.general	t	2020-01-22 21:46:59.89208-08
2	Order	nce.category.order	t	2020-01-22 21:46:59.89208-08
3	Sample	nce.category.sample	t	2020-01-22 21:46:59.89208-08
4	Analysis	nce.category.analysis	t	2020-01-22 21:46:59.89208-08
5	Post-Analytical	nce.category.postAnalytical	t	2020-01-22 21:46:59.89208-08
\.


--
-- Name: nce_category_id_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.nce_category_id_seq', 5, true);


--
-- Data for Name: nce_specimen; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.nce_specimen (id, nce_id, sample_item_id) FROM stdin;
\.


--
-- Name: nce_specimen_id_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.nce_specimen_id_seq', 1, false);


--
-- Data for Name: nce_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.nce_type (id, name, display_key, category_id, active, last_updated) FROM stdin;
1	Documentation error	nce.type.documentationError	1	t	2020-01-22 21:46:59.934738-08
2	Employee concern	nce.type.employeeConcern	1	t	2020-01-22 21:46:59.934738-08
3	Technology/Computer issue	nce.type.technologyIssue	1	t	2020-01-22 21:46:59.934738-08
4	Other, please describe	nce.type.otherDescribe	1	t	2020-01-22 21:46:59.934738-08
5	Demographic error	nce.type.demographicError	2	t	2020-01-22 21:46:59.934738-08
6	Form missing	nce.type.formMissing	2	t	2020-01-22 21:46:59.934738-08
7	Form incorrect	nce.type.formIncorrect	2	t	2020-01-22 21:46:59.934738-08
8	Transmission Delay	nce.type.transmissionDelay	2	t	2020-01-22 21:46:59.934738-08
9	Barcode Issue	nce.type.barcodeIssue	3	t	2020-01-22 21:46:59.934738-08
10	Broken Tube/Container	nce.type.brokenTubeContainer	3	t	2020-01-22 21:46:59.934738-08
11	Coagulated Sample	nce.type.coagulatedSample	3	t	2020-01-22 21:46:59.934738-08
12	Contamination - client	nce.type.contaminationClient	3	t	2020-01-22 21:46:59.934738-08
13	Contamination - internal	nce.type.contaminationInternal	3	t	2020-01-22 21:46:59.934738-08
14	Hemolytic sample	nce.type.haemolyticSample	3	t	2020-01-22 21:46:59.934738-08
15	Improper Specimen Collection	nce.type.improperSpecimenCollection	3	t	2020-01-22 21:46:59.934738-08
16	Inadequate Sampling	nce.type.inadequateSampling	3	t	2020-01-22 21:46:59.934738-08
17	Insufficient Amount	nce.type.insufficientAmount	3	t	2020-01-22 21:46:59.934738-08
18	Leakage	nce.type.leakage	3	t	2020-01-22 21:46:59.934738-08
19	Lost Specimen - Irreplaceable	nce.type.lostSpecimenIrreplacable	3	t	2020-01-22 21:46:59.934738-08
20	Lost Specimen - Redraw	nce.type.lostSpecimenRedraw	3	t	2020-01-22 21:46:59.934738-08
21	Mislabeled/unlabeled specimen	nce.type.mislabeledUnlabeled	3	t	2020-01-22 21:46:59.934738-08
22	Missing Sample	nce.type.missingSample	3	t	2020-01-22 21:46:59.934738-08
23	Specimen Receipt Delay	nce.type.specimenReceiptDelay	3	t	2020-01-22 21:46:59.934738-08
24	Spill	nce.type.spill	3	t	2020-01-22 21:46:59.934738-08
25	Wrong specimen	nce.type.wrongSpecimen	3	t	2020-01-22 21:46:59.934738-08
26	Wrong temperature	nce.type.wrongTemperature	3	t	2020-01-22 21:46:59.934738-08
27	Calibration Issue	nce.type.calibrationIssue	4	t	2020-01-22 21:46:59.934738-08
28	Equipment Failure	nce.type.equipmentFailure	4	t	2020-01-22 21:46:59.934738-08
29	Expired Reagent/Material	nce.type.expiredReagentMaterial	4	t	2020-01-22 21:46:59.934738-08
30	Incorrect measurement of sample or reagent	nce.type.incorrectMeasurement	4	t	2020-01-22 21:46:59.934738-08
31	Instrument Downtime	nce.type.instrumentDowntime	4	t	2020-01-22 21:46:59.934738-08
32	Inventory Storage	nce.type.inventoryStorage	4	t	2020-01-22 21:46:59.934738-08
33	Lot Number Issue	nce.type.lotNumberIssue	4	t	2020-01-22 21:46:59.934738-08
34	Wrong Test Kit	nce.type.wrongTestKit	4	t	2020-01-22 21:46:59.934738-08
35	Rerun - Equipment Failure	nce.type.rerunEquipmentFailure	4	t	2020-01-22 21:46:59.934738-08
36	Rerun - Inconsistent Results	nce.type.rerunInconsistentResults	4	t	2020-01-22 21:46:59.934738-08
37	Rerun- QC Failure	nce.type.rerunQCFailure	4	t	2020-01-22 21:46:59.934738-08
38	SOP Deviation (please describe)	nce.type.sopDeviation	4	t	2020-01-22 21:46:59.934738-08
39	Result reported when quality control sample tested out of range	nce.type.resultReported	5	t	2020-01-22 21:46:59.934738-08
\.


--
-- Name: nce_type_id_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.nce_type_id_seq', 39, true);


--
-- Data for Name: note; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.note (id, sys_user_id, reference_id, reference_table, note_type, subject, text, lastupdated) FROM stdin;
\.


--
-- Name: note_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.note_seq', 1, false);


--
-- Data for Name: observation_history; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.observation_history (id, patient_id, sample_id, observation_history_type_id, value_type, value, lastupdated, sample_item_id) FROM stdin;
\.


--
-- Name: observation_history_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.observation_history_seq', 290, true);


--
-- Data for Name: observation_history_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.observation_history_type (id, type_name, description, lastupdated) FROM stdin;
1	initialSampleCondition	The condition of the sample when it was delievered to the lab	2011-02-16 14:48:45.08403-08
2	paymentStatus	The payment status of the patient	2012-03-13 15:32:16.615986-07
3	requestDate	When the order was requested	2012-06-11 09:33:46.997733-07
4	nextVisitDate	When the patient should return	2012-06-11 09:33:46.997733-07
5	referringSite	Non-lab referring organization	2012-06-11 09:33:46.997733-07
6	primaryOrderType	What kind of order is this	2012-06-11 09:33:46.997733-07
7	secondaryOrderType	The sub order kind	2012-06-11 09:33:46.997733-07
8	otherSecondaryOrderType	The sub order kind	2012-06-11 09:33:46.997733-07
9	billingRefNumber	Number used to track billing	2020-01-22 21:46:11.017212-08
10	program	What program the order is in	2020-01-22 21:46:11.045698-08
11	educationLevel	Education Level	2020-01-22 21:46:41.413949-08
12	maritalStatus	Marital Status	2020-01-22 21:46:41.413949-08
13	nationality	Nationality	2020-01-22 21:46:41.413949-08
14	legalResidence	Legal Residence	2020-01-22 21:46:41.413949-08
15	SampleRecordStatus	Sample Record Status	2020-01-22 21:46:41.413949-08
16	PatientRecordStatus	Patient Record Status	2020-01-22 21:46:41.413949-08
17	nameOfDoctor	Name of Doctor	2020-01-22 21:46:41.413949-08
18	anyPriorDiseases	Any Prior Diseases	2020-01-22 21:46:41.413949-08
19	priorDiseases	Prior Diseases	2020-01-22 21:46:41.413949-08
20	anyCurrentDiseases	Any Current Diseases	2020-01-22 21:46:41.413949-08
21	currentDiseases	Current Diseases	2020-01-22 21:46:41.413949-08
22	arvProphylaxisBenefit	Arv Prophylaxis Benefit	2020-01-22 21:46:41.413949-08
23	arvProphylaxis	Arv Prophylaxis	2020-01-22 21:46:41.413949-08
24	currentARVTreatment	Current ARV Treatment	2020-01-22 21:46:41.413949-08
25	priorARVTreatment	Prior ARV Treatment	2020-01-22 21:46:41.413949-08
26	priorARVTreatmentINNs	Prior ARV Treatment Medicines (INN)	2020-01-22 21:46:41.413949-08
27	cotrimoxazoleTreatment	Cotrimoxazole Treatment	2020-01-22 21:46:41.413949-08
28	aidsStage	AIDS Stage	2020-01-22 21:46:41.413949-08
29	currentOITreatment	Current Opportunistic Infection Treatment	2020-01-22 21:46:41.413949-08
30	patientWeight	Patient Weight	2020-01-22 21:46:41.413949-08
31	karnofskyScore	Karnofsky Score	2020-01-22 21:46:41.413949-08
32	whichPCR	Which PCR - 1st or 2nd Test	2020-01-22 21:46:41.413949-08
33	reasonForSecondPCRTest	Reason for Second PCR Test	2020-01-22 21:46:41.413949-08
34	eidInfantPTME	Infant Benefit from PTME	2020-01-22 21:46:41.413949-08
35	eidTypeOfClinic	Type of Clinic	2020-01-22 21:46:41.413949-08
36	eidInfantSymptomatic	Infant Experiencing Symptoms	2020-01-22 21:46:41.413949-08
37	eidMothersHIVStatus	Mother's HIV Status	2020-01-22 21:46:41.413949-08
38	eidMothersARV	Mother's ARV Treatment	2020-01-22 21:46:41.413949-08
39	eidInfantsARV	Infant's ARV Prophylaxis	2020-01-22 21:46:41.413949-08
40	eidInfantCotrimoxazole	Infant Cotrimoxazole	2020-01-22 21:46:41.413949-08
41	indFirstTestName	Indeterminate First Test Name	2020-01-22 21:46:41.413949-08
42	indSecondTestName	Indeterminate Second Test Name	2020-01-22 21:46:41.413949-08
43	indFirstTestDate	Indeterminate First Test Date	2020-01-22 21:46:41.413949-08
44	indSecondTestDate	Indeterminate Second Test Date	2020-01-22 21:46:41.413949-08
45	indFirstTestResult	Indeterminate First Test Result	2020-01-22 21:46:41.413949-08
46	indSecondTestResult	Indeterminate Second Test Result	2020-01-22 21:46:41.413949-08
47	indSiteFinalResult	Indeterminate Site Final Result	2020-01-22 21:46:41.413949-08
48	eidHowChildFed	How the infant is eating	2020-01-22 21:46:41.413949-08
49	cd4Count	CD4 Count	2020-01-22 21:46:41.413949-08
50	cd4Percent	CD4 Percent	2020-01-22 21:46:41.413949-08
51	priorCd4Date	prior CD4 Date	2020-01-22 21:46:41.413949-08
52	antiTbTreatment	anti TB Treatment	2020-01-22 21:46:41.413949-08
53	interruptedARVTreatment	Interrupted ARV Treatment	2020-01-22 21:46:41.413949-08
54	arvTreatmentAnyAdverseEffects	arv Treatment Any AdverseEffects	2020-01-22 21:46:41.413949-08
55	arvTreatmentChange	arv Treatment Change	2020-01-22 21:46:41.413949-08
56	arvTreatmentNew	arv Treatment New	2020-01-22 21:46:41.413949-08
57	arvTreatmentRegime	arv Treatment Regime	2020-01-22 21:46:41.413949-08
58	cotrimoxazoleTreatAnyAdvEff	Cotrimoxazole Treatment Any Adverse Effects	2020-01-22 21:46:41.413949-08
59	anySecondaryTreatment	Any Secondary Treatment	2020-01-22 21:46:41.413949-08
60	secondaryTreatment	Secondary Treatment	2020-01-22 21:46:41.413949-08
61	clinicVisits	ClinicVisits	2020-01-22 21:46:41.413949-08
62	hospital	Hospital	2020-01-22 21:46:41.413949-08
63	service	Service in Hospital	2020-01-22 21:46:41.413949-08
64	hospitalPatient	Hospital Patient	2020-01-22 21:46:41.413949-08
65	futureARVTreatmentINNs	 Future ARV Treatment Medicine/Drugs	2020-01-22 21:46:41.413949-08
66	arvTreatmentAdvEffGrd	 ARV Treatment Adverse Effects: Grade	2020-01-22 21:46:41.413949-08
67	arvTreatmentAdvEffType	 ARV Treatment Adverse Effects: Type	2020-01-22 21:46:41.413949-08
68	cotrimoxazoleTreatAdvEffGrd	 Cotrimoxazole Treatment Adverse Effects: Grade	2020-01-22 21:46:41.413949-08
69	cotrimoxazoleTreatAdvEffType	 Cotrimoxazole Treatment Adverse Effects: Type	2020-01-22 21:46:41.413949-08
70	projectFormName	the name of the form (paper questions not JSP) which the patient used.	2020-01-22 21:46:41.413949-08
71	hivStatus	HIV Status	2020-01-22 21:46:41.413949-08
72	reason	Reason that this sample/patient test request was created.	2020-01-22 21:46:41.413949-08
73	nameOfSampler	Name of Sampler	2020-01-22 21:46:41.413949-08
74	nameOfRequestor	Name of Requestor	2020-01-22 21:46:41.413949-08
75	eidTypeOfClinicOther	text field for undefined clinic type	2020-01-22 21:46:41.413949-08
76	eidStoppedBreastfeeding	value for when the child stopped breastfeeding	2020-01-22 21:46:41.413949-08
77	underInvestigation	indicates that this patient/sample combination is under investigation	2020-01-22 21:46:41.413949-08
78	CTBPul	Pulmonary TB : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
79	CTBExpul	Expulmonary TB : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
80	CCrblToxo	Cerebral Toxoplamosis : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
81	CCryptoMen	Cryoptococcal Meningitis : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
82	CGenPrurigo	General Prurigo : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
83	CIST	IST (?) : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
84	CCervCancer	Cervical Cancer : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
85	COpharCand	Oropharyngeal Candidiasis : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
86	CKaposiSarc	Kaposi's sarcoma : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
87	CShingles	shingles : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
88	CDiarrheaC	Chronic Diarrhea : Answer to (current) disease question	2020-01-22 21:46:41.413949-08
89	PTBPul	Pulmonary TB : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
90	PTBExpul	Expulmonary TB : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
91	PCrblToxo	Cerebral Toxoplamosis : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
92	PCryptoMen	Cryoptococcal Meningitis : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
93	PGenPrurigo	General Prurigo : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
94	PIST	IST (?) : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
95	PCervCancer	Cervical Cancer : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
96	POpharCand	Oropharyngeal Candidiasis : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
97	PKaposiSarc	Kaposi's sarcoma : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
98	PShingles	shingles : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
99	PDiarrheaC	Chronic Diarrhea : Answer to (prior) disease question	2020-01-22 21:46:41.413949-08
100	weightLoss	weight loss: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
101	diarrhea	diarrhea: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
102	fever	fever: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
103	cough	cough: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
104	pulTB	pulmonary Tuberculosis: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
105	expulTB	expulmonary Tuberculosis: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
106	swallPaint	painful Swallowing: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
107	cryptoMen	cryptococcus Meningitis: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
108	recPneumon	recurrent Pneumonia: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
109	sespis	sespis : Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
110	recInfect	recurrent Infections: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
111	curvixC	curvix Cancer: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
112	matHIV	maternal HIV: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
113	cachexie	cachexia: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
114	thrush	thrush : Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
115	dermPruip	General Dermititis Pruipineuse: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
116	herpes	Herpes : Answer to RTN Disease Question: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
117	zona	Zona: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
118	sarcKapo	Kaposis Sarcoma: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
119	xIngPadenp	Extra Inquinale Polyadenopathy: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
120	HIVDement	HIV Dementia: Answer to RTN Disease Question	2020-01-22 21:46:41.413949-08
121	arvTreatmentInitDate	Date of first ARV treatment initiation	2020-01-22 21:46:41.413949-08
122	currentARVTreatmentINNs	Therapeutic Regimen	2020-01-22 21:46:41.413949-08
123	vlReasonForRequest	Reason for VL Request	2020-01-22 21:46:41.413949-08
124	vlOtherReasonForRequest	Other Reason for VL Request	2020-01-22 21:46:41.413949-08
125	initcd4Count	CD4 at first ARV treatment initiation	2020-01-22 21:46:41.413949-08
126	initcd4Percent	CD4 percent at first ARV treatment initiation	2020-01-22 21:46:41.413949-08
127	initcd4Date	Date CD4 at first ARV treatment initiation	2020-01-22 21:46:41.413949-08
128	demandcd4Count	CD4 at demand	2020-01-22 21:46:41.413949-08
129	demandcd4Percent	CD4 percentat demand	2020-01-22 21:46:41.413949-08
130	demandcd4Date	Date CD4 at demand	2020-01-22 21:46:41.413949-08
131	vlBenefit	Prior VL Request?	2020-01-22 21:46:41.413949-08
132	priorVLLab	Prior VL Lab	2020-01-22 21:46:41.413949-08
133	priorVLValue	Prior VL Value	2020-01-22 21:46:41.413949-08
134	priorVLDate	Prior VL Date	2020-01-22 21:46:41.413949-08
135	vlPregnancy	VL Pregnancy?	2020-01-22 21:46:42.121252-08
136	vlSuckle	VL Suckle?	2020-01-22 21:46:42.121252-08
\.


--
-- Name: observation_history_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.observation_history_type_seq', 136, true);


--
-- Name: occupation_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.occupation_seq', 1, false);


--
-- Data for Name: or_properties; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.or_properties (property_id, property_key, property_value) FROM stdin;
1870	base.directory	/usr/share/tomcat5.5/webapps/openreports/reports/
1871	temp.directory	/usr/share/tomcat5.5/webapps/openreports/temp/
1872	report.generation.directory	/usr/share/tomcat5.5/webapps/openreports/generatedreports/
1873	date.format	dd/MM/yyyy
1874	mail.auth.password	barLAC28
1875	mail.auth.user	admin
1876	mail.smtp.auth	false
1877	mail.smtp.host	
1878	xmla.catalog	
1879	xmla.datasource	
1880	xmla.uri	
\.


--
-- Data for Name: or_tags; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.or_tags (tag_id, tagged_object_id, tagged_object_class, tag_value, tag_type) FROM stdin;
\.


--
-- Data for Name: order_item; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.order_item (id, ord_id, quantity_requested, quantity_received, inv_loc_id) FROM stdin;
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.orders (id, org_id, sys_user_id, ordered_date, neededby_date, requested_by, cost_center, shipping_type, shipping_carrier, shipping_cost, delivered_date, is_external, external_order_number, is_filled) FROM stdin;
\.


--
-- Data for Name: org_hl7_encoding_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.org_hl7_encoding_type (organization_id, encoding_type_id, lastupdated) FROM stdin;
\.


--
-- Data for Name: org_mls_lab_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.org_mls_lab_type (org_id, mls_lab_id, org_mlt_id) FROM stdin;
\.


--
-- Data for Name: organization; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.organization (id, name, city, zip_code, mls_sentinel_lab_flag, org_mlt_org_mlt_id, org_id, short_name, multiple_unit, street_address, state, internet_address, clia_num, pws_id, lastupdated, mls_lab_flag, is_active, local_abbrev, code, datim_org_code, datim_org_name) FROM stdin;
7	Inconnu		\N	N	\N	\N				MN		\N	\N	2011-02-25 11:19:16.449	N	Y	\N	\N	\N	\N
3	CI	Seattle	98103	N	\N	\N			DNA	WA			\N	2008-11-20 13:48:42.141	N	Y	22	\N	\N	\N
1329	AGNEBY-TIASSA-ME	\N	\N	N	\N	\N	1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1336	GBOKLE-NAWA-SAN PÉDRO	\N	\N	N	\N	\N	2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1394	N’ZI-IFOU	\N	\N	N	\N	\N	12	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1449	CENTRE DE SANTE DE KORHOGO	\N	\N	N	\N	\N		\N	\N	\N	\N	\N	\N	2012-07-09 16:57:36.952	\N	Y	\N	\N	\N	\N
1342	KABADOUGOU-BAFIN-FOLON	\N	\N	N	\N	\N	3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1345	HAUT SASSANDRA	\N	\N	N	\N	\N	4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1349	GOH	\N	\N	N	\N	\N	5	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1352	BELIER	\N	\N	N	\N	\N	6	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1357	ABIDJAN 1-GRANDS PONTS	\N	\N	N	\N	\N	7	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1367	ABIDJAN 2	\N	\N	N	\N	\N	8	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1378	MARAHOUE	\N	\N	N	\N	\N	9	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1382	TONPKI	\N	\N	N	\N	\N	10	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1387	CAVALLY-GUEMON	\N	\N	N	\N	\N	11	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1401	INDENIE-DJUABLIN	\N	\N	N	\N	\N	13	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1405	PORO-TCHOLOGO-BAGOUE	\N	\N	N	\N	\N	14	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1417	GBÈKÈ	\N	\N	N	\N	\N	17	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1399	M’BAHIAKRO	\N	\N	N	\N	1394	12.5	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1410	LOH-DJIBOUA	\N	\N	N	\N	\N	15	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1413	SUD-COMOE	\N	\N	N	\N	\N	16	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1423	HAMBOL	\N	\N	N	\N	\N	18	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1427	WORODOUGOU-BERE	\N	\N	N	\N	\N	19	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1430	BOUNKANI-GONTOUGO	\N	\N	N	\N	\N	20	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1330	ADZOPE	\N	\N	N	\N	1329	1.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1331	AGBOVILLE	\N	\N	N	\N	1329	1.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1332	AKOUPE	\N	\N	N	\N	1329	1.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1333	ALEPE	\N	\N	N	\N	1329	1.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1334	SIKENSI	\N	\N	N	\N	1329	1.5	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1335	TIASSALE	\N	\N	N	\N	1329	1.6	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1337	SAN-PEDRO	\N	\N	N	\N	1336	2.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1338	TABOU	\N	\N	N	\N	1336	2.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1339	SOUBRE	\N	\N	N	\N	1336	2.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1340	SASSANDRA	\N	\N	N	\N	1336	2.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1341	GUEYO	\N	\N	N	\N	1336	2.5	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1343	ODIENNE	\N	\N	N	\N	1342	3.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1344	TOUBA	\N	\N	N	\N	1342	3.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1346	DALOA	\N	\N	N	\N	1345	4.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1347	VAVOUA	\N	\N	N	\N	1345	4.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1348	ISSIA	\N	\N	N	\N	1345	4.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1350	GAGNOA	\N	\N	N	\N	1349	5.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1351	OUME	\N	\N	N	\N	1349	5.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1353	YAMOUSSOUKRO	\N	\N	N	\N	1352	6.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1354	TIEBISSOU	\N	\N	N	\N	1352	6.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1355	TOUMODI	\N	\N	N	\N	1352	6.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1356	DIDIEVI	\N	\N	N	\N	1352	6.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1358	ADJAME	\N	\N	N	\N	1357	7.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1359	PLATEAU	\N	\N	N	\N	1357	7.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1360	ATTECOUBE	\N	\N	N	\N	1357	7.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1361	YOUPOUGON EST	\N	\N	N	\N	1357	7.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1362	YOPOUGON OUST	\N	\N	N	\N	1357	7.5	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1363	SONGON	\N	\N	N	\N	1357	7.6	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1364	DABOU	\N	\N	N	\N	1357	7.7	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1365	JACQUEVILLE	\N	\N	N	\N	1357	7.8	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1366	GRAND-LAHOU	\N	\N	N	\N	1357	7.9	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1368	TREICHVILLE	\N	\N	N	\N	1367	8.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1369	MARCORY	\N	\N	N	\N	1367	8.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1370	KOUMASSI	\N	\N	N	\N	1367	8.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1371	PORT-BOUET	\N	\N	N	\N	1367	8.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1372	VRIDI	\N	\N	N	\N	1367	8.5	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1373	COCODY	\N	\N	N	\N	1367	8.6	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1374	BINGERVILLE	\N	\N	N	\N	1367	8.7	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1375	ABOBO EST	\N	\N	N	\N	1367	8.8	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1376	ABOBO OUEST	\N	\N	N	\N	1367	8.9	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1377	ANYAMA	\N	\N	N	\N	1367	8.10	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1379	BOUAFLE	\N	\N	N	\N	1378	9.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1380	SINFRA	\N	\N	N	\N	1378	9.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1381	ZUENOULA	\N	\N	N	\N	1378	9.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1383	MAN	\N	\N	N	\N	1382	10.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1384	BIANKOUMAN	\N	\N	N	\N	1382	10.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1385	DANANE	\N	\N	N	\N	1382	10.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1386	ZOUAN-HOUNIEN	\N	\N	N	\N	1382	10.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1388	GUIGLO	\N	\N	N	\N	1387	11.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1389	TOULEUPLEU	\N	\N	N	\N	1387	11.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1390	DUEKOUE	\N	\N	N	\N	1387	11.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1391	BLOLEQUIN	\N	\N	N	\N	1387	11.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1392	BANGOLO	\N	\N	N	\N	1387	11.5	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1393	KOUIBLY	\N	\N	N	\N	1387	11.6	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1395	DIMBOKRO	\N	\N	N	\N	1394	12.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1396	BOCANDA	\N	\N	N	\N	1394	12.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1397	BONGOUANOU	\N	\N	N	\N	1394	12.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1398	DAOUKRO	\N	\N	N	\N	1394	12.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1400	PRIKRO	\N	\N	N	\N	1394	12.6	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1402	ABENGOUROU	\N	\N	N	\N	1401	13.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1403	AGNIBILEKRO	\N	\N	N	\N	1401	13.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1404	BETTIE	\N	\N	N	\N	1401	13.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1406	KORHOGO	\N	\N	N	\N	1405	14.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1407	BOUDIALI	\N	\N	N	\N	1405	14.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1408	FERKESSEDOUGOU	\N	\N	N	\N	1405	14.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1409	TENGRELA	\N	\N	N	\N	1405	14.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1411	DIVO	\N	\N	N	\N	1410	15.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1412	LAKOTA	\N	\N	N	\N	1410	15.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1414	ABOISSO	\N	\N	N	\N	1413	16.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1415	ADIAKE	\N	\N	N	\N	1413	16.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1416	GRAND-BASSAM	\N	\N	N	\N	1413	16.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1418	BOUAKE NORD-OUEST	\N	\N	N	\N	1417	17.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1419	BOUAKE NORD-EST	\N	\N	N	\N	1417	17.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1420	BOUAKE SUD	\N	\N	N	\N	1417	17.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1421	SAKASSOU	\N	\N	N	\N	1417	17.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1422	BEOUMI	\N	\N	N	\N	1417	17.5	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1424	DABAKALA	\N	\N	N	\N	1423	18.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1425	KATIOLA	\N	\N	N	\N	1423	18.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1426	NIAKARAMADOUGOU	\N	\N	N	\N	1423	18.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1428	SEGUELA	\N	\N	N	\N	1427	19.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1429	MANKONO	\N	\N	N	\N	1427	19.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1431	BONDOUKOU	\N	\N	N	\N	1430	20.1	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1432	BOUNA	\N	\N	N	\N	1430	20.2	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1433	TANDA	\N	\N	N	\N	1430	20.3	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1434	NASSIAN	\N	\N	N	\N	1430	20.4	\N	\N	\N	\N	\N	\N	2012-06-05 16:56:26.981998	\N	Y	\N	\N	\N	\N
1447	PAUL'S HOUSE	\N	\N	N	\N	\N		\N	\N	\N	\N	\N	\N	2012-07-06 17:17:30.841	\N	Y	\N	\N	\N	\N
1448	FRED	\N	\N	N	\N	\N		\N	\N	\N	\N	\N	\N	2012-07-06 17:18:37.351	\N	Y	\N	\N	\N	\N
1450	PAGODA	\N	\N	N	\N	\N		\N	\N	\N	\N	\N	\N	2012-10-25 16:54:47.62	\N	Y	\N	\N	\N	\N
1451	BALLARD	\N	\N	N	\N	\N		\N	\N	\N	\N	\N	\N	2012-10-26 09:44:19.382	\N	Y	\N	\N	\N	\N
1452	JANS SITE	\N	\N	N	\N	\N		\N	\N	\N	\N	\N	\N	2013-02-05 09:56:21.261	\N	Y	\N	\N	\N	\N
759	Retro-CI	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	\N	\N	\N	\N	\N
760	ABENGOUROU	\N	\N	N	\N	\N	AG	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
761	CAT ADJAME	\N	\N	N	\N	\N	CA	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
762	CIRBA	\N	\N	N	\N	\N	CI	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
763	HOPITAL GENERAL DE DANANE	\N	\N	N	\N	\N	HD	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
764	HOPITAL MILITAIRE	\N	\N	N	\N	\N	HM	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
765	MALADIES INFECTIEUSES	\N	\N	N	\N	\N	MI	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
766	LA PIERRE ANGULAIRE	\N	\N	N	\N	\N	PA	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
767	PPH CHU COCODY	\N	\N	N	\N	\N	PC	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
768	PEDIATRIE CHU YOPOUGON	\N	\N	N	\N	\N	PY	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
769	AMBASSADE DES USA	\N	\N	N	\N	\N	UE	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
770	CENTRE EXPERENCE	\N	\N	N	\N	\N	UM	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
771	U.S.A.C.	\N	\N	N	\N	\N	US	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
772	HG TOULEUPLEU	\N	\N	N	\N	\N	284	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
773	CSU MEAGUI	\N	\N	N	\N	\N	241	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
774	CSU YABAYO	\N	\N	N	\N	\N	242	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
775	CSU MAYO	\N	\N	N	\N	\N	243	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
776	CSU Okrouyo	\N	\N	N	\N	\N	244	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
777	CSU Grand Zatry	\N	\N	N	\N	\N	245	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
778	CSR Méné Centre	\N	\N	N	\N	\N	246	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
779	CSU Gabiadji	\N	\N	N	\N	\N	247	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
780	CSR Touih	\N	\N	N	\N	\N	248	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
781	CSR Djapadji	\N	\N	N	\N	\N	249	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
782	CSR Dagadi	\N	\N	N	\N	\N	250	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
783	CSR Kako	\N	\N	N	\N	\N	251	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
784	SAPH TOUPAH	\N	\N	N	\N	\N	252	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
785	CSU TOUPAH	\N	\N	N	\N	\N	253	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
786	CSR Cosrou	\N	\N	N	\N	\N	254	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
787	CSR Viel Aklodj	\N	\N	N	\N	\N	255	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
788	CSR Ellibou	\N	\N	N	\N	\N	256	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
789	CSU Gomon	\N	\N	N	\N	\N	257	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
790	CSR BECEDI	\N	\N	N	\N	\N	258	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
791	CSR BAKANOU B	\N	\N	N	\N	\N	259	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
792	CSU GADOUAN	\N	\N	N	\N	\N	260	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
793	CSU ZOUKOUGBEU	\N	\N	N	\N	\N	261	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
794	CSR ZALIHOUAN	\N	\N	N	\N	\N	262	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
795	CSU BEDIALA	\N	\N	N	\N	\N	263	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
796	CSR BOBOUA BAHOUAN	\N	\N	N	\N	\N	264	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
797	ICT GR YAMOUSSOUKRO	\N	\N	N	\N	\N	265	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
798	CS PIETRO BONELLI	\N	\N	N	\N	\N	266	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
799	CSU TIEME	\N	\N	N	\N	\N	267	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
800	PMI/MATERNITE ODIENNE	\N	\N	N	\N	\N	268	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
801	HG MADINANI	\N	\N	N	\N	\N	269	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
802	CM COMPLEXE SUCRIER BOROTOU	\N	\N	N	\N	\N	270	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
803	HG BANGOLO	\N	\N	N	\N	\N	271	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
804	HG ZOUHAN HOUNIEN	\N	\N	N	\N	\N	272	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
805	HG BIANKOUMAN	\N	\N	N	\N	\N	273	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
806	HG DANANE	\N	\N	N	\N	\N	274	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
807	CSU MAHAPLEU	\N	\N	N	\N	\N	275	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
808	HG DUEKOUE	\N	\N	N	\N	\N	276	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
809	HG KOUIBLY	\N	\N	N	\N	\N	277	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
810	CSU FACOBLY	\N	\N	N	\N	\N	278	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
811	CAMES MAN	\N	\N	N	\N	\N	279	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
812	CSU Logoualé	\N	\N	N	\N	\N	280	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
813	HG BLOLEQUIN	\N	\N	N	\N	\N	281	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
814	CHC (Centre Hévéïcole du CAVALLY)	\N	\N	N	\N	\N	282	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
815	CSU ZAGNE	\N	\N	N	\N	\N	283	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
816	FSU COM ABOBO BAOULE	\N	\N	N	\N	\N	117	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
817	FSUCOM KOWEIT	\N	\N	N	\N	\N	334	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
818	FSU COM GESCO	\N	\N	N	\N	\N	335	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
819	FSU COM ABOBOTE	\N	\N	N	\N	\N	336	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
820	CHU  Yopougon  Pédiatrie	\N	\N	N	\N	\N	107	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
821	FSU COM PORT-BOUET 2	\N	\N	N	\N	\N	108	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
822	Disp. Sœur Cathérine Yopougon	\N	\N	N	\N	\N	109	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
823	FSU COM Toit rouge	\N	\N	N	\N	\N	110	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
824	FSU COM Wassakara	\N	\N	N	\N	\N	111	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
825	Centre Nazaréen Yopougon	\N	\N	N	\N	\N	112	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
826	MTCT PLUS NIANGON	\N	\N	N	\N	\N	113	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
827	HG ANYAMA	\N	\N	N	\N	\N	114	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
828	HG ABOBO SUD	\N	\N	N	\N	\N	115	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
829	FSU COM Anonkoua Kouté abobo	\N	\N	N	\N	\N	116	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
830	MTCT Plus ABOBO AVOCATIER	\N	\N	N	\N	\N	118	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
831	HG SIKENSI	\N	\N	N	\N	\N	119	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
832	HG DABOU	\N	\N	N	\N	\N	120	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
833	PHCI COSROU	\N	\N	N	\N	\N	121	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
834	CSU LOPOU	\N	\N	N	\N	\N	122	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
835	POLYCLINIQUE II PLATEAUX	\N	\N	N	\N	\N	145	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
836	CS Abobo Te	\N	\N	N	\N	\N	146	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
837	PMI YAMOUSSOUKRO	\N	\N	N	\N	\N	147	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
838	CHR YAMOUSSOUKRO	\N	\N	N	\N	\N	148	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
839	HG TOUMODI	\N	\N	N	\N	\N	149	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
840	HG DJEKANOU	\N	\N	N	\N	\N	150	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
841	CHR DALOA	\N	\N	N	\N	\N	151	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
842	PMI DALOA	\N	\N	N	\N	\N	152	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
843	HG SOUBRE	\N	\N	N	\N	\N	153	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
844	CHR GAGNOA	\N	\N	N	\N	\N	154	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
845	PMI GAGNOA	\N	\N	N	\N	\N	155	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
846	DU GAGNOA	\N	\N	N	\N	\N	156	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
847	CAMES Yopougon	\N	\N	N	\N	\N	157	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
848	PMI GRAND-BASSAM	\N	\N	N	\N	\N	158	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
849	CSR SAMO	\N	\N	N	\N	\N	159	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
850	CSR YAOU	\N	\N	N	\N	\N	160	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
851	HG GRAND BASSAM	\N	\N	N	\N	\N	161	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
852	CSU NOE	\N	\N	N	\N	\N	162	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
853	HG ADIAKE	\N	\N	N	\N	\N	163	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
854	HG BONOUA	\N	\N	N	\N	\N	164	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
855	La Pierre angulaire	\N	\N	N	\N	\N	165	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
856	HG Abobo Nord	\N	\N	N	\N	\N	10	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
857	FSU Yopougon Attié	\N	\N	N	\N	\N	15	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
858	CEPREF	\N	\N	N	\N	\N	16	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
859	Hopit. Methodiste Dabou	\N	\N	N	\N	\N	18	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
860	HG Sassandra	\N	\N	N	\N	\N	19	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
861	Maternité Cafétou-Abeng	\N	\N	N	\N	\N	30	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
862	CSU KOUTO	\N	\N	N	\N	\N	326	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
863	CSR BAYA	\N	\N	N	\N	\N	327	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
864	CSU KASSERE	\N	\N	N	\N	\N	328	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
865	CSU GBON	\N	\N	N	\N	\N	329	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
866	HG Ayamé	\N	\N	N	\N	\N	46	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
867	HG Grand Lahou	\N	\N	N	\N	\N	47	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
868	HG Jacqueville	\N	\N	N	\N	\N	48	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
869	CSC PALAZOLLO	\N	\N	N	\N	\N	189	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
870	CSU TANGUELAN	\N	\N	N	\N	\N	190	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
871	CSU AKOBOISSUE	\N	\N	N	\N	\N	191	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
872	FSU Plateau	\N	\N	N	\N	\N	49	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
873	Maternité Attécoubé	\N	\N	N	\N	\N	50	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
874	CSU COM Agban village	\N	\N	N	\N	\N	51	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
875	MATERNITE WILLIAMSVILLE	\N	\N	N	\N	\N	52	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
876	CSU SANKADIOKRO	\N	\N	N	\N	\N	192	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
877	CSR DIAMAROKRO	\N	\N	N	\N	\N	193	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
878	CSU BETTIE	\N	\N	N	\N	\N	194	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
879	CSU NIABLE	\N	\N	N	\N	\N	195	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
880	PMI PALMERAIE	\N	\N	N	\N	\N	196	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
881	FSU Marcory	\N	\N	N	\N	\N	8	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
882	Centre El Rapha Abobo	\N	\N	N	\N	\N	9	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
883	CSR Moussadougou	\N	\N	N	\N	\N	228	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
884	HG Koumassi	\N	\N	N	\N	\N	11	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
885	SAINTE THERESE-KOUMASSI	\N	\N	N	\N	\N	12	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
886	CSU Grabo	\N	\N	N	\N	\N	233	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
887	HG Port Bouet	\N	\N	N	\N	\N	13	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
888	CSU COM Gonzagueville	\N	\N	N	\N	\N	14	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
889	CSR Dogbo	\N	\N	N	\N	\N	229	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
890	CSR Goh	\N	\N	N	\N	\N	230	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
891	FSU Locodjro	\N	\N	N	\N	\N	17	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
892	PMI KOKO	\N	\N	N	\N	\N	123	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
893	SAINT CAMILLE BOUAKE	\N	\N	N	\N	\N	124	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
894	CSU BELLE VILLE	\N	\N	N	\N	\N	125	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
895	CSU DJEZOUKOUAMEKRO	\N	\N	N	\N	\N	126	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
896	HG M’BAHIAKRO	\N	\N	N	\N	\N	127	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
897	HG FERKESSEDOUGOU	\N	\N	N	\N	\N	128	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
898	HOPITAL BAPTISTE FERKE	\N	\N	N	\N	\N	129	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
899	HG OUANGOLODOUGOU	\N	\N	N	\N	\N	130	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
900	HG BOUNDIALI	\N	\N	N	\N	\N	131	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
901	HG DABAKALA	\N	\N	N	\N	\N	132	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
902	HG KATIOLA	\N	\N	N	\N	\N	133	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
903	SUCAF2 KATIOLA	\N	\N	N	\N	\N	134	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
904	HG ADZOPE	\N	\N	N	\N	\N	135	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
905	PMI ADZOPE	\N	\N	N	\N	\N	136	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
906	HG BINGERVILLE	\N	\N	N	\N	\N	137	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
907	CM MARIA ELISA	\N	\N	N	\N	\N	138	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
908	PMI AGBOVILLE	\N	\N	N	\N	\N	139	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
909	CHR AGBOVILLE	\N	\N	N	\N	\N	140	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
910	CM WALE YAMOUSSOUKRO	\N	\N	N	\N	\N	141	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
911	RSB YAMOUSSOUKRO	\N	\N	N	\N	\N	142	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
912	CSU COM VRIDI CITE	\N	\N	N	\N	\N	143	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
913	HG DELAFOSSE	\N	\N	N	\N	\N	144	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
914	CSR Olodio	\N	\N	N	\N	\N	236	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
915	HG Agnibilékrou	\N	\N	N	\N	\N	31	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
916	PMI AGNIBILEKROU	\N	\N	N	\N	\N	32	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
917	HG Akoupé	\N	\N	N	\N	\N	33	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
918	HG Tabou	\N	\N	N	\N	\N	231	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
919	Centre social Tabou	\N	\N	N	\N	\N	232	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
920	CM SUCAF1 FERKE	\N	\N	N	\N	\N	323	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
921	CSU M'BATTO	\N	\N	N	\N	\N	324	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
922	PMI de BARDOT	\N	\N	N	\N	\N	22	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
923	CHR de San Pedro	\N	\N	N	\N	\N	23	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
924	Maternité Bardot	\N	\N	N	\N	\N	24	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
925	SOGB-GRAND BEREBY	\N	\N	N	\N	\N	25	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
926	Centre SAS - Bouake	\N	\N	N	\N	\N	26	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
927	CHR Abengourou	\N	\N	N	\N	\N	27	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
928	PMI ABENGOUROU	\N	\N	N	\N	\N	28	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
929	PIM ABENGOUROU	\N	\N	N	\N	\N	29	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
930	CHR ABENG. PEDIATRIE	\N	\N	N	\N	\N	220	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
931	CSR BLESSEGUE	\N	\N	N	\N	\N	325	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
932	CHR ABENG. MATERNITE	\N	\N	N	\N	\N	221	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
933	FSU Terre rouge	\N	\N	N	\N	\N	222	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
934	FSU Zimbabwé	\N	\N	N	\N	\N	223	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
935	Centre social San-Pédro	\N	\N	N	\N	\N	224	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
936	CMS Hévégo	\N	\N	N	\N	\N	225	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
937	CSU Grand-Béréby	\N	\N	N	\N	\N	226	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
938	CMS SAPH Rapid Grah	\N	\N	N	\N	\N	227	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
939	DR Ménéké	\N	\N	N	\N	\N	235	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
940	CSU Djouroutou	\N	\N	N	\N	\N	234	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
941	FSU Attecoube	\N	\N	N	\N	\N	1	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
942	CSU Williamsville	\N	\N	N	\N	\N	53	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
943	INSP	\N	\N	N	\N	\N	54	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
944	FSU COM Abobo Sagbé	\N	\N	N	\N	\N	55	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
945	CSU COM Kennedy Clouetcha	\N	\N	N	\N	\N	56	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
946	CSU COM ABOBO Anonkoua3	\N	\N	N	\N	\N	57	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
947	PEDIATRIE CHU COCODY	\N	\N	N	\N	\N	4	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
948	CSU COM Anono	\N	\N	N	\N	\N	5	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
949	FSU Abobo Doume	\N	\N	N	\N	\N	2	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
950	PEDIATRIE CHU TREICHVILLE	\N	\N	N	\N	\N	7	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
951	CSU BECEDI BRIGNAN	\N	\N	N	\N	\N	178	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
952	CSU ASSIKOI	\N	\N	N	\N	\N	179	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
953	CSU AGOU	\N	\N	N	\N	\N	180	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
954	CSR YAKASSE ME	\N	\N	N	\N	\N	181	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
955	CSR ABOUDE	\N	\N	N	\N	\N	182	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
956	CSU RUBINO	\N	\N	N	\N	\N	183	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
957	CSU AZAGUIE	\N	\N	N	\N	\N	184	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
958	CMS BANA COMOE	\N	\N	N	\N	\N	185	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
959	CSU AFFERY	\N	\N	N	\N	\N	186	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
960	CSU DAME	\N	\N	N	\N	\N	187	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
961	CSR ASSUAME	\N	\N	N	\N	\N	188	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
962	HG Daoukro	\N	\N	N	\N	\N	34	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
963	PMI DAOUKRO	\N	\N	N	\N	\N	35	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
964	CHR Dimbokro	\N	\N	N	\N	\N	36	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
965	PMI DIMBOKRO	\N	\N	N	\N	\N	37	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
966	CSUC NDA Dimbokro	\N	\N	N	\N	\N	38	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
967	PMI TIASSALE	\N	\N	N	\N	\N	39	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
968	HG Tiassale	\N	\N	N	\N	\N	40	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
969	CSU N'Douci	\N	\N	N	\N	\N	41	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
970	CSU N'Ziannouan	\N	\N	N	\N	\N	42	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
971	HG Taabo	\N	\N	N	\N	\N	43	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
972	HG Bocanda	\N	\N	N	\N	\N	44	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
973	HG Bongouanou	\N	\N	N	\N	\N	45	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
974	CSH KB Blockhauss	\N	\N	N	\N	\N	297	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
975	CENTRE PLUS YOPOUGON	\N	\N	N	\N	\N	298	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
976	CIRBA	\N	\N	N	\N	\N	299	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
977	HOPE CASM TREICHVILLE	\N	\N	N	\N	\N	300	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
978	KO’KHOUA CNTS TREICHVILLE	\N	\N	N	\N	\N	301	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
979	CSU COM ANGRE	\N	\N	N	\N	\N	302	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
980	AIBEF TREICHVILLE	\N	\N	N	\N	\N	303	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
981	CSU COM BOCABO	\N	\N	N	\N	\N	304	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
982	CSU COM BANCO SUD	\N	\N	N	\N	\N	305	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
983	Disp. Charité KOTOBI	\N	\N	N	\N	\N	306	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
984	SAPH BONGO	\N	\N	N	\N	\N	307	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
985	CSU KARAKORO	\N	\N	N	\N	\N	308	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
986	CSR KIEMOU	\N	\N	N	\N	\N	309	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
987	CSR KAFIOKAHA	\N	\N	N	\N	\N	310	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
988	CSR BORON	\N	\N	N	\N	\N	311	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
989	FSU COM ALIODAN	\N	\N	N	\N	\N	312	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
990	DR KONI	\N	\N	N	\N	\N	313	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
991	PMI KORHOGO	\N	\N	N	\N	\N	314	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
992	MATERNITE PETIT PARIS	\N	\N	N	\N	\N	315	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
993	CSU KOKOTON	\N	\N	N	\N	\N	316	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
994	CSU KOMBORO	\N	\N	N	\N	\N	317	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
995	Disp. BAPTISTE TORGOKAHA	\N	\N	N	\N	\N	318	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
996	CSU KONG	\N	\N	N	\N	\N	319	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
997	CSU NIELE	\N	\N	N	\N	\N	320	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
998	CSU DIAWALA	\N	\N	N	\N	\N	321	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
999	DSR NAMBINGUE	\N	\N	N	\N	\N	322	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1000	CSR Sago	\N	\N	N	\N	\N	20	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1001	CSR Bolo SIPEFCI	\N	\N	N	\N	\N	21	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1002	CSU DUALLA	\N	\N	N	\N	\N	106	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1003	CSU Guibéroua	\N	\N	N	\N	\N	3	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1004	AIBEF Daloa	\N	\N	N	\N	\N	6	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1005	HG BOUAFLE	\N	\N	N	\N	\N	58	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1006	Maternité Dioulabougou BOUAFLE	\N	\N	N	\N	\N	59	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1007	PMI BOUAFLE	\N	\N	N	\N	\N	60	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1008	Centre Social BOUAFLE	\N	\N	N	\N	\N	61	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1009	CSU Pakouabo	\N	\N	N	\N	\N	62	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1010	CSU Bonon	\N	\N	N	\N	\N	63	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1011	HG Issia	\N	\N	N	\N	\N	64	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1012	PMI ISSIA	\N	\N	N	\N	\N	65	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1013	CSU Bazre	\N	\N	N	\N	\N	66	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1014	CSU Ibogué	\N	\N	N	\N	\N	67	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1015	CSU Saioua	\N	\N	N	\N	\N	68	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1016	CSU NAHIO	\N	\N	N	\N	\N	69	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1017	CSU BOGUEDIA	\N	\N	N	\N	\N	70	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1018	HG SINFRA	\N	\N	N	\N	\N	71	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1019	DU Christ Roi	\N	\N	N	\N	\N	72	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1020	CSI Kayeta	\N	\N	N	\N	\N	73	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1021	CSU KONONFLA	\N	\N	N	\N	\N	74	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1022	HG ZUENOULA	\N	\N	N	\N	\N	75	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1023	CMS Sucrivoire ZUENOULA	\N	\N	N	\N	\N	76	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1024	District de Zuenoula	\N	\N	N	\N	\N	77	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1025	CSU GOHITAFLA	\N	\N	N	\N	\N	78	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1026	HG VAVOUA	\N	\N	N	\N	\N	79	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1027	CSU BONOUFLA	\N	\N	N	\N	\N	80	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1028	CSR BAZRA NATIS	\N	\N	N	\N	\N	81	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1029	HG LAKOTA	\N	\N	N	\N	\N	82	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1030	CSU KOUDOULILIE	\N	\N	N	\N	\N	83	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1031	CSU NIAKOBLOGNOA	\N	\N	N	\N	\N	84	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1032	CSU NIAMBEZARIA	\N	\N	N	\N	\N	85	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1033	CSU HIRE	\N	\N	N	\N	\N	86	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1034	CSU GUITRY	\N	\N	N	\N	\N	87	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1035	PMI CATHOLIQUE GUITRY	\N	\N	N	\N	\N	88	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1036	CSU YOCOBOUE	\N	\N	N	\N	\N	89	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1037	CSU HERMANKONO GARE	\N	\N	N	\N	\N	90	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1038	HG FRESCO	\N	\N	N	\N	\N	91	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1039	CSR GBAGBAM	\N	\N	N	\N	\N	92	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1040	CM Soeur Franzisca de  Gbagbam	\N	\N	N	\N	\N	93	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1041	PMI OUME	\N	\N	N	\N	\N	94	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1042	Centre Social OUME	\N	\N	N	\N	\N	95	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1043	CSU DIEGONEFLA	\N	\N	N	\N	\N	96	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1044	Notre Dame de la Consolata - Marandallah	\N	\N	N	\N	\N	97	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1045	HG MANKONO	\N	\N	N	\N	\N	98	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1046	CSU DIBROSSO	\N	\N	N	\N	\N	99	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1047	CSR GABIA OUME	\N	\N	N	\N	\N	100	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1048	Centre Social LAKOTA	\N	\N	N	\N	\N	101	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1049	CSU KONAHIRI	\N	\N	N	\N	\N	102	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1050	CSU KONGASSO	\N	\N	N	\N	\N	103	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1051	CSU DIANRA	\N	\N	N	\N	\N	104	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1052	CSU KANI	\N	\N	N	\N	\N	382	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1053	CSU GONATE	\N	\N	N	\N	\N	168	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1054	CSU GUESSABO	\N	\N	N	\N	\N	169	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1055	CSU GBOGUHE	\N	\N	N	\N	\N	170	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1056	CSU GARAGE	\N	\N	N	\N	\N	171	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1057	MATERNITE MUNICIPALE DALOA	\N	\N	N	\N	\N	172	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1058	CSR Broma	\N	\N	N	\N	\N	173	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1059	CSR GABIA ISSIA	\N	\N	N	\N	\N	174	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1060	CSR Golihoa	\N	\N	N	\N	\N	175	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1061	CSR Luehouan	\N	\N	N	\N	\N	176	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1062	CSR Niaka	\N	\N	N	\N	\N	177	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1063	CSU HERMANKONO DIES	\N	\N	N	\N	\N	197	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1064	CSR TIEGBA	\N	\N	N	\N	\N	198	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1065	CSR Akabia	\N	\N	N	\N	\N	199	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1066	CSR Grobiakoko	\N	\N	N	\N	\N	200	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1067	CSR Chiepo	\N	\N	N	\N	\N	201	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1068	CSU Tonla	\N	\N	N	\N	\N	202	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1069	CSR Sakahouho	\N	\N	N	\N	\N	203	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1070	CSR Doukouya	\N	\N	N	\N	\N	204	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1071	CSR Zaddi	\N	\N	N	\N	\N	205	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1072	CSU Galebre	\N	\N	N	\N	\N	206	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1073	CSU Seriho	\N	\N	N	\N	\N	207	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1074	CSU Gnagbodougnoa	\N	\N	N	\N	\N	208	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1075	CSR Kragbalilie	\N	\N	N	\N	\N	209	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1076	CSR Dignago	\N	\N	N	\N	\N	210	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1077	CSU Dania	\N	\N	N	\N	\N	211	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1078	CSR Baoulifla	\N	\N	N	\N	\N	212	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1079	Centre social Daloa	\N	\N	N	\N	\N	213	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1080	CSU ZIKISSO	\N	\N	N	\N	\N	214	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1081	CSU Tieningboue	\N	\N	N	\N	\N	215	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1082	CSU Sarala	\N	\N	N	\N	\N	216	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1083	CSU Sifie	\N	\N	N	\N	\N	217	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1084	CSU Worofla	\N	\N	N	\N	\N	218	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1085	CSU Guepahouo	\N	\N	N	\N	\N	219	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1086	CSR HUAFLA	\N	\N	N	\N	\N	237	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1087	CSI KOUETINFLA	\N	\N	N	\N	\N	238	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1088	CSR DJENEDOUFLA	\N	\N	N	\N	\N	239	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1089	CSR BINZRA	\N	\N	N	\N	\N	285	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1090	CSR MANFLA	\N	\N	N	\N	\N	286	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1091	CSR ZOROFLA	\N	\N	N	\N	\N	287	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1092	CSR ZANZRA	\N	\N	N	\N	\N	288	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1093	CSR DANANON	\N	\N	N	\N	\N	289	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1094	CSR ANCIEN PROSI	\N	\N	N	\N	\N	290	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1095	CSR GBABO	\N	\N	N	\N	\N	291	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1096	CSR ZAGUIETA	\N	\N	N	\N	\N	292	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1097	CSR BOZI	\N	\N	N	\N	\N	293	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1098	CSR SIETINFLA	\N	\N	N	\N	\N	294	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1099	CSR MASSARALA	\N	\N	N	\N	\N	295	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1100	CSR MORONDO	\N	\N	N	\N	\N	296	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1101	CSR KETRO BASSAM	\N	\N	N	\N	\N	331	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1102	CSR PELEZI	\N	\N	N	\N	\N	332	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1103	CSR TRAFESSO	\N	\N	N	\N	\N	333	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1104	MALADIES INFECTIEUSES	\N	\N	N	\N	\N	1	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1105	PPH	\N	\N	N	\N	\N	2	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1106	DERMATOLOGIE	\N	\N	N	\N	\N	3	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1107	MEDECINE INTERNE	\N	\N	N	\N	\N	4	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1108	NEUROLOGIE	\N	\N	N	\N	\N	5	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1109	PEDIATRIE	\N	\N	N	\N	\N	6	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1110	CTRO	\N	\N	N	\N	\N	7	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1111	GYNECOLOGIE	\N	\N	N	\N	\N	8	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1112	CHIRURGIE	\N	\N	N	\N	\N	9	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1113	AUTRE	\N	\N	N	\N	\N	10	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1114	ABENGOUROU	\N	\N	N	\N	\N	11	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1115	BOUAKE	\N	\N	N	\N	\N	12	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1116	DALOA	\N	\N	N	\N	\N	13	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1117	GAGNOA	\N	\N	N	\N	\N	14	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1118	KORHOGO	\N	\N	N	\N	\N	15	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1119	MAN	\N	\N	N	\N	\N	16	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1120	AUTRES INTERIEUR	\N	\N	N	\N	\N	17	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1121	CANCEROLOGIE	\N	\N	N	\N	\N	18	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1122	CARDIOLOGIE	\N	\N	N	\N	\N	19	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1123	U.S.A.C.	\N	\N	N	\N	\N	20	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1124	INHP	\N	\N	N	\N	\N	21	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1125	CHU Treichville	\N	\N	N	\N	\N	1	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1126	CHU Cocody	\N	\N	N	\N	\N	2	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1127	CHU Yopougon	\N	\N	N	\N	\N	3	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1128	AUTRES ABIDJAN	\N	\N	N	\N	\N	4	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1129	Hôpitaux intérieurs	\N	\N	N	\N	\N	5	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.507549	\N	Y	\N	\N	\N	\N
1132	USAC	\N	\N	N	\N	\N	355	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1133	TESTING ARV	\N	\N	N	\N	\N	6666	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1134	TESTINGRTN	\N	\N	N	\N	\N	77777	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1135	TESTING RTN HOSPITALS	\N	\N	N	\N	\N	333	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1136	PMI SOKOURA	\N	\N	N	\N	\N	343	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1137	CHU TREICHVILLE - PEDIATRIE	\N	\N	N	\N	\N	PT	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	N	\N	\N	\N	\N
1138	Charge Virale	\N	\N	N	\N	\N	CV	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1139	Pediatrie CHU-Treichville	\N	\N	N	\N	\N	PT	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1140	CSU SOKOURA	\N	\N	N	\N	\N	356	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1141	CSU KOTTIA KOFFIKRO	\N	\N	N	\N	\N	357	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1142	CSU ZATTA	\N	\N	N	\N	\N	358	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1143	CSU KOSSOU	\N	\N	N	\N	\N	359	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1144	CSR LABOKRO	\N	\N	N	\N	\N	360	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1145	CSU LOLOBO	\N	\N	N	\N	\N	361	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1146	CSU DIOULABOUGOU	\N	\N	N	\N	\N	362	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1147	CSU ATTIEGOUAKRO	\N	\N	N	\N	\N	363	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1148	CSU TIMBE	\N	\N	N	\N	\N	364	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1149	DR SAKIARE	\N	\N	N	\N	\N	365	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1150	DR N'ZERE	\N	\N	N	\N	\N	366	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1151	HG TIEBISSOU	\N	\N	N	\N	\N	367	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1152	HG DIDIEVI	\N	\N	N	\N	\N	368	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1153	HG SAKASOU	\N	\N	N	\N	\N	369	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1154	CSR ASSANDRE	\N	\N	N	\N	\N	370	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1155	CSR N'GUESSAN POKOUKRO	\N	\N	N	\N	\N	371	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1156	PMI FERKE	\N	\N	N	\N	\N	372	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1157	DR SEMAN	\N	\N	N	\N	\N	373	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1158	CSU MOROFE	\N	\N	N	\N	\N	374	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1159	CSU N'ZUESSY	\N	\N	N	\N	\N	375	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1160	CSU KOKOUMBO	\N	\N	N	\N	\N	376	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1161	CSU ANGODA	\N	\N	N	\N	\N	377	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1162	CSU KPOUEBO	\N	\N	N	\N	\N	378	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1163	FS POLICE DALOA	\N	\N	N	\N	\N	379	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1164	CMS NOTRE DAME DU CALVAIRE GUIBEROUA	\N	\N	N	\N	\N	380	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1165	CNPS DIVO	\N	\N	N	\N	\N	381	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1166	CSU SEITIFLA	\N	\N	N	\N	\N	105	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1167	DR ADJAMENE	\N	\N	N	\N	\N	383	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1168	DR BLAHOU	\N	\N	N	\N	\N	384	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1169	DR NERO BROUSE	\N	\N	N	\N	\N	385	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1170	DR BLIGBEADJI	\N	\N	N	\N	\N	386	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1171	CSU DOBA	\N	\N	N	\N	\N	387	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1172	CSR PARA	\N	\N	N	\N	\N	388	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1173	DR YOUKOU	\N	\N	N	\N	\N	389	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1174	CMS NEKA	\N	\N	N	\N	\N	390	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1175	CMS IBOKE PALMCI	\N	\N	N	\N	\N	391	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1176	CMS GBAPET	\N	\N	N	\N	\N	392	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1177	CMS BLIDOUBA	\N	\N	N	\N	\N	393	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1178	DR SOUBLAKE	\N	\N	N	\N	\N	394	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1179	DR GOZON	\N	\N	N	\N	\N	395	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1180	CSR TIEVIESSOU	\N	\N	N	\N	\N	396	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1181	CSR AHOUANOU	\N	\N	N	\N	\N	397	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1182	CSR EBONOU	\N	\N	N	\N	\N	398	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1183	CSU BACANDA	\N	\N	N	\N	\N	399	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1184	CSR LAHOU KPANDA	\N	\N	N	\N	\N	400	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1185	CSR BRAFFEDON	\N	\N	N	\N	\N	401	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1186	CSR TOUKOUZOU	\N	\N	N	\N	\N	402	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1187	CSR GRAND ADKOUZIN	\N	\N	N	\N	\N	403	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1188	CSU AZAGUIE ADZOPE	\N	\N	N	\N	\N	404	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1189	CENTRE SOCIAL ADZOPE	\N	\N	N	\N	\N	405	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1190	CSR AHEREMOU II	\N	\N	N	\N	\N	406	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1191	CSR SOKROGBO CARREFOUR	\N	\N	N	\N	\N	407	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1192	DR BOTINDE	\N	\N	N	\N	\N	408	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1193	DR AMANIMENOU	\N	\N	N	\N	\N	409	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1194	DR ASSEUDJI	\N	\N	N	\N	\N	410	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1195	CSR ANANGUIE AGBOVILLE	\N	\N	N	\N	\N	411	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1196	CSR ERIMAKOUDJEL	\N	\N	N	\N	\N	412	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1197	ORES KROBOU	\N	\N	N	\N	\N	413	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1198	CSU APPROMPRONOU	\N	\N	N	\N	\N	414	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1199	Disp. MS Christ Roi	\N	\N	N	\N	\N	415	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1200	CSR ABIE	\N	\N	N	\N	\N	416	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1201	CSR AMORIAKRO	\N	\N	N	\N	\N	418	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1202	CSR ANANGUIE ADZOPE	\N	\N	N	\N	\N	419	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1203	CSR ANDE	\N	\N	N	\N	\N	420	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1204	CSR ANNO	\N	\N	N	\N	\N	421	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1205	CSR BANGOUA	\N	\N	N	\N	\N	422	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1206	CSR ATTOBROU	\N	\N	N	\N	\N	423	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1207	CSU KOUASSI DATEKRO	\N	\N	N	\N	\N	424	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1208	CSR BOUDEPE	\N	\N	N	\N	\N	425	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1209	CSR SEKREKE TABOU	\N	\N	N	\N	\N	426	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1210	CM NIMATOULLAH	\N	\N	N	\N	\N	427	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1211	CSR DIAPE	\N	\N	N	\N	\N	428	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1212	CSR DIASSON	\N	\N	N	\N	\N	429	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1213	CSR DJAMADIOKE TABOU	\N	\N	N	\N	\N	430	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1214	DISP. RURAL MAHINO	\N	\N	N	\N	\N	431	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1215	CENTRE SANTE AKOUEDO	\N	\N	N	\N	\N	432	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1216	CP ZIMBAWE	\N	\N	N	\N	\N	433	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1217	CSS ANGE GARDIEN	\N	\N	N	\N	\N	434	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1218	CMS SCASO	\N	\N	N	\N	\N	435	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1219	PALMCI IROBO (CMS YOCOBOBOUE)	\N	\N	N	\N	\N	436	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1220	CSR LIBOLI GRAND LAHOU	\N	\N	N	\N	\N	437	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1221	CSR THERESE HB NIGUI SAFF	\N	\N	N	\N	\N	438	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1222	CSU YAKASSE ATTOBROU	\N	\N	N	\N	\N	439	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1223	CSU CSR BIEBY	\N	\N	N	\N	\N	440	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1224	CSU ABONGOUA ATTIE	\N	\N	N	\N	\N	441	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1225	CSR BIECOUEFIN	\N	\N	N	\N	\N	442	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1226	CSR AFOTOBO	\N	\N	N	\N	\N	443	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1227	CSR BODOKRO	\N	\N	N	\N	\N	444	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1228	CSR MARABADIASSA	\N	\N	N	\N	\N	445	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1229	CSR ASSIRIKRO	\N	\N	N	\N	\N	697	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1230	CSU COM ARRAS	\N	\N	N	\N	\N	447	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1231	CSR BAMORO	\N	\N	N	\N	\N	448	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1232	CSU BROBO	\N	\N	N	\N	\N	449	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1233	CSR LANGBASSOU	\N	\N	N	\N	\N	450	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1234	CSR ASSOUAKRO	\N	\N	N	\N	\N	451	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1235	CSU COM PANGOLIN	\N	\N	N	\N	\N	452	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1236	CSR LENGBRE	\N	\N	N	\N	\N	453	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1237	CSU DJEBONOUA	\N	\N	N	\N	\N	454	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1238	CSU DOROPO	\N	\N	N	\N	\N	455	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1239	CSR SATAMA-SOKORA	\N	\N	N	\N	\N	456	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1240	DR BOKALA	\N	\N	N	\N	\N	457	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1241	CSU GRAND MORIE AGBOVILLE	\N	\N	N	\N	\N	458	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1242	CSR PACOBO	\N	\N	N	\N	\N	459	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1243	CSR KOMBOLOKOURA	\N	\N	N	\N	\N	460	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1244	CSU KOLIA	\N	\N	N	\N	\N	461	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1245	CSU BINAO BOUSSOUE	\N	\N	N	\N	\N	462	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1246	DR SOHOUO	\N	\N	N	\N	\N	463	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1247	CSU KOUTOUBA	\N	\N	N	\N	\N	464	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1248	CSU LAOUDIBA	\N	\N	N	\N	\N	465	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1249	CSU LOVIGUIE	\N	\N	N	\N	\N	466	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1250	CSR PAGUEKAHA	\N	\N	N	\N	\N	467	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1251	CSR BOLONA	\N	\N	N	\N	\N	468	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1252	CSR LOMARA	\N	\N	N	\N	\N	469	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1253	CHR BONDOUKOU (SCE Pediatrie)	\N	\N	N	\N	\N	470	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1254	CSU SIEMPURGO	\N	\N	N	\N	\N	471	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1255	CIP CAMES	\N	\N	N	\N	\N	472	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1256	CSR SOKO	\N	\N	N	\N	\N	473	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1257	HOPITAL GENERAL ABOBO NORD	\N	\N	N	\N	\N	AN	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1258	CSU TEHINI	\N	\N	N	\N	\N	474	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1259	CSR KOUASSI N'DAWA'	\N	\N	N	\N	\N	475	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1260	INSTITUT RAOUL FOLLEREAU	\N	\N	N	\N	\N	476	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1261	DISP. DERAHOUAN DALOA	\N	\N	N	\N	\N	477	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1262	CSR KONG	\N	\N	N	\N	\N	478	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1263	DR FLAKIEDOUGOU	\N	\N	N	\N	\N	479	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1264	CSR YEZIMALA	\N	\N	N	\N	\N	480	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1265	CENTRE SAINTE MARIE	\N	\N	N	\N	\N	481	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1266	CMS FLAMBOYANT	\N	\N	N	\N	\N	482	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1267	DR ANNEPE	\N	\N	N	\N	\N	483	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1268	DR BIASSO	\N	\N	N	\N	\N	484	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1269	CSR BOUNDA	\N	\N	N	\N	\N	485	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1270	CSR DASSOUNGBO	\N	\N	N	\N	\N	486	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1271	CSR BOUKO	\N	\N	N	\N	\N	487	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1272	FSU COCODY	\N	\N	N	\N	\N	488	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1273	CSR PARHADI	\N	\N	N	\N	\N	489	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1274	HG NASSIAN	\N	\N	N	\N	\N	493	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1275	HMA	\N	\N	N	\N	\N	494	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1276	HOPITAL POLICE NATIONALE (HPN)	\N	\N	N	\N	\N	495	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1277	DR DIANGOBO	\N	\N	N	\N	\N	496	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1278	DR BONAHOUIN	\N	\N	N	\N	\N	500	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1279	DR YADIO	\N	\N	N	\N	\N	502	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1280	CSR GUEZON	\N	\N	N	\N	\N	503	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1281	CSR KOUANHOULE	\N	\N	N	\N	\N	504	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1282	CSR GBINTA	\N	\N	N	\N	\N	505	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1283	CMS THANRY	\N	\N	N	\N	\N	506	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1284	CSU APPOISSO	\N	\N	N	\N	\N	507	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1285	CSU ANIASSUE	\N	\N	N	\N	\N	508	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1286	CSU ZARANOU	\N	\N	N	\N	\N	509	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1287	CSU AFFALIKRO	\N	\N	N	\N	\N	510	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1288	CSU AMELEKIA	\N	\N	N	\N	\N	511	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1289	CSU AMIANKOUASSIKRO	\N	\N	N	\N	\N	512	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1290	CSR SATIKRAN	\N	\N	N	\N	\N	513	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1291	CSR MANZANOUA	\N	\N	N	\N	\N	514	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1292	CSU DOUFFREBO	\N	\N	N	\N	\N	515	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1293	DR YOBOUAKRO	\N	\N	N	\N	\N	516	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1294	HG KOUNFAO	\N	\N	N	\N	\N	517	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1295	CSU TANKESSE	\N	\N	N	\N	\N	518	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1296	HG TANDA	\N	\N	N	\N	\N	519	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1297	CSU ASSUEFRY	\N	\N	N	\N	\N	520	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1298	CSU TRANSUA	\N	\N	N	\N	\N	521	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1299	CSR ABONGOUA	\N	\N	N	\N	\N	522	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1300	CSU KANGANDI	\N	\N	N	\N	\N	523	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1301	CSU ANOUMABA	\N	\N	N	\N	\N	524	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1302	HG PRIKRO	\N	\N	N	\N	\N	525	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1303	CSU COM AKOMIABLA	\N	\N	N	\N	\N	526	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1304	CSU COM KOUMASSI DIVO	\N	\N	N	\N	\N	527	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1305	CSU COM GRAND CAMPEMENT	\N	\N	N	\N	\N	528	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1306	CSU COM ANOUMABO	\N	\N	N	\N	\N	529	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1307	CSU VRIDI CANAL	\N	\N	N	\N	\N	530	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1308	CSU VRIDI 3	\N	\N	N	\N	\N	531	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1309	MATERNITE 220 LOGEMENTS	\N	\N	N	\N	\N	532	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1310	ASAPSU YAO SEI	\N	\N	N	\N	\N	533	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1311	CSR MOAPE	\N	\N	N	\N	\N	534	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1312	CSR MOROKO	\N	\N	N	\N	\N	535	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1313	CSR SOKROBO	\N	\N	N	\N	\N	536	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1314	CSR KOTIESSOU	\N	\N	N	\N	\N	537	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1315	CSR SONGON	\N	\N	N	\N	\N	538	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1316	CSU BIANOUAN	\N	\N	N	\N	\N	539	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1317	ASAPSU ALIODAN	\N	\N	N	\N	\N	540	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1318	CSU COM WILLIAMSVILLE	\N	\N	N	\N	\N	541	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1319	PMI KATIOLA	\N	\N	N	\N	\N	542	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1320	CSU FRONAN	\N	\N	N	\N	\N	543	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1321	CSU NIAKARA	\N	\N	N	\N	\N	544	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1322	CSU TORTIYA	\N	\N	N	\N	\N	545	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1323	CSU TAFIRE	\N	\N	N	\N	\N	546	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1324	CSR SIKOLO	\N	\N	N	\N	\N	547	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1325	CSR KAOURA	\N	\N	N	\N	\N	548	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1326	CSR NAMBONKAHA	\N	\N	N	\N	\N	549	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1327	CSU KOUMBALA	\N	\N	N	\N	\N	550	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1328	CSR TOUMOUKORO	\N	\N	N	\N	\N	551	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1600	PEDIATRIE HG BOUNDIALI	\N	\N	N	\N	\N	552	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1601	CSU DIABO	\N	\N	N	\N	\N	337	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1602	CSR BENDE KOUASSIKRO	\N	\N	N	\N	\N	338	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1603	CSR ANGOUA YAOKRO	\N	\N	N	\N	\N	339	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1604	CSU BOTRO	\N	\N	N	\N	\N	340	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1605	HG BEOUMI	\N	\N	N	\N	\N	341	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1606	CSU NIMBO	\N	\N	N	\N	\N	342	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1607	CSU BASSAWA	\N	\N	N	\N	\N	344	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1608	CSU BONIERE	\N	\N	N	\N	\N	345	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1609	CSU FOUMBOLO	\N	\N	N	\N	\N	346	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1610	CSR SATAMA-SOKORO	\N	\N	N	\N	\N	347	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1611	PMI BONDOUKOU	\N	\N	N	\N	\N	348	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1612	CSU SAPLI SEPINGO	\N	\N	N	\N	\N	349	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1613	CSU GOUMERE	\N	\N	N	\N	\N	350	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1614	HG BOUNA	\N	\N	N	\N	\N	351	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1615	CSU KANOROBA	\N	\N	N	\N	\N	352	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1616	CSU GUIEMBE	\N	\N	N	\N	\N	353	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1617	CSU TIORO	\N	\N	N	\N	\N	354	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1618	PMI COCODY	\N	\N	N	\N	\N	166	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1619	CHU BOUAKE	\N	\N	N	\N	\N	240	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1620	OMS	\N	\N	N	\N	\N	OMS	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1621	FSU AHOUGNANSSOU	\N	\N	N	\N	\N	330	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1622	CHU YOPOUGON	\N	\N	N	\N	\N	CHUY	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1623	ESPACE CONFIANCE	\N	\N	N	\N	\N	EC	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1624	Gyneco PTME Cocody	\N	\N	N	\N	\N	GPC	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1625	CSR VAAFLA	\N	\N	N	\N	\N	492	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1626	HG OUME	\N	\N	N	\N	\N	491	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1627	CARITAS St Anne de Port Bouet	\N	\N	N	\N	\N	554	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1628	DR NIAPIDOU	\N	\N	N	\N	\N	565	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1629	CSR GNAGO 2	\N	\N	N	\N	\N	564	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1630	CSU SONGON YOP.	\N	\N	N	\N	\N	555	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1631	CSR NIGROUGBOUE	\N	\N	N	\N	\N	563	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1632	CSR ADEBEM	\N	\N	N	\N	\N	574	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1633	DR GOBROKO	\N	\N	N	\N	\N	572	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1634	CMM GSPM	\N	\N	N	\N	\N	MS	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1635	CSR N'GUATTY	\N	\N	N	\N	\N	557	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1636	CSM Derriere WHARF	\N	\N	N	\N	\N	553	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1637	CSU BAKO	\N	\N	N	\N	\N	570	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1638	CSU KANIASSO	\N	\N	N	\N	\N	578	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1639	DR SAHOUA	\N	\N	N	\N	\N	573	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1640	CSU LOBAKUYA	\N	\N	N	\N	\N	571	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1641	DR DREWIN	\N	\N	N	\N	\N	575	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1642	CSR DAPKADOU	\N	\N	N	\N	\N	561	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1643	CSR KOKOLOPOZO	\N	\N	N	\N	\N	562	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1644	CSR NIABABLY	\N	\N	N	\N	\N	566	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1645	CSR SARAKADJI	\N	\N	N	\N	\N	577	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1646	CSR BALEKO	\N	\N	N	\N	\N	569	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1647	HG GUEYO	\N	\N	N	\N	\N	567	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1648	CPS AGBAN	\N	\N	N	\N	\N	560	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1649	CSU DABOUYO	\N	\N	N	\N	\N	568	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1650	CSU AIR FRANCE	\N	\N	N	\N	\N	594	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1651	DR SAFA MANOIS	\N	\N	N	\N	\N	576	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1652	CS CATHOLIQUE DJEBONOUA	\N	\N	N	\N	\N	595	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1653	FSU COM ABOBO AVOCATIER	\N	\N	N	\N	\N	558	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1654	MATERNITE URBAINE BELLE	\N	\N	N	\N	\N	602	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1655	CSR YAOU ABOISSO	\N	\N	N	\N	\N	499	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1656	INFIRMERIE PRIVEE RAHAMA TOULAHI	\N	\N	N	\N	\N	599	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1657	CSR NAMASSI	\N	\N	N	\N	\N	611	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1658	CMM GSPM INDENIE	\N	\N	N	\N	\N	556	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1659	CSU DAR ES SALAM	\N	\N	N	\N	\N	596	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1660	MATERNITE CSR WORA	\N	\N	N	\N	\N	601	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1661	CSR EBOUNOU	\N	\N	N	\N	\N	398	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1662	CSU KETESSO	\N	\N	N	\N	\N	498	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1663	CSR SOROBANGO	\N	\N	N	\N	\N	606	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1664	CSU TAOUDI	\N	\N	N	\N	\N	608	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1665	FSU COM BC	\N	\N	N	\N	\N	616	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1666	FSU COM AKEIKOI	\N	\N	N	\N	\N	617	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1667	ABOBO CES CLOTCHA	\N	\N	N	\N	\N	618	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1668	H G ABOBO SUD	\N	\N	N	\N	\N	AS	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1669	CENTRE SAS BOUAKE	\N	\N	N	\N	\N	CB	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1670	CSU ZAIBO	\N	\N	N	\N	\N	490	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1671	CSR EBIKRO N'DAKRO	\N	\N	N	\N	\N	497	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1672	CSU ANDO - KEKRENOU	\N	\N	N	\N	\N	591	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1673	CSR SAMINIKRO	\N	\N	N	\N	\N	592	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1674	CNT CROIX ROUGE KORHOGO	\N	\N	N	\N	\N	598	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1675	CSR GBANHUI	\N	\N	N	\N	\N	609	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1676	CSU SANDEGUE	\N	\N	N	\N	\N	610	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1677	CESACO	\N	\N	N	\N	\N	600	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1678	CSR KOUKOURANDOUMI	\N	\N	N	\N	\N	580	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1679	CSR GAZIBOUO	\N	\N	N	\N	\N	582	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1680	ONG ACID	\N	\N	N	\N	\N	620	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1681	Centre Medical PABB du Tresor	\N	\N	N	\N	\N	619	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1682	Disp. Rural YOUNDOUO	\N	\N	N	\N	\N	613	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1683	DR SEGUIE BOGUIE	\N	\N	N	\N	\N	586	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1684	FSU COM PORT BOUET 2	\N	\N	N	\N	\N	PB	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1685	DR OFFA	\N	\N	N	\N	\N	585	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1686	Dispensaire Rural de M'BROME'	\N	\N	N	\N	\N	641	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1687	WOLLOH	\N	\N	N	\N	\N	597	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1688	HMIAC ATTIENKRO	\N	\N	N	\N	\N	647	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1689	CSR ANGOVIA	\N	\N	N	\N	\N	662	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1690	CSR N'DOUFOUKANKRO	\N	\N	N	\N	\N	661	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1691	CSR LOLOBO	\N	\N	N	\N	\N	590	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1692	MATERNITE KASSIRIMI	\N	\N	N	\N	\N		\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1693	MATERNITE KASSIRIME	\N	\N	N	\N	\N	655	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1694	CSR ALLOKODJEKRO	\N	\N	N	\N	\N	698	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1695	CSR MAPAIX	\N	\N	N	\N	\N	581	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1696	CENTRE ESPACE CONFIANCE	\N	\N	N	\N	\N	615	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1697	CSR GOLIKPANGBASSOU	\N	\N	N	\N	\N	644	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1698	CSR DRIBOUO	\N	\N	N	\N	\N	665	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1699	CSU SONGON YOPOUGON	\N	\N	N	\N	\N	555	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1700	CSR BONDO	\N	\N	N	\N	\N	656	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1701	CSR DIGBAPIA	\N	\N	N	\N	\N	658	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1702	Neurologie CHU Cocody	\N	\N	N	\N	\N	4	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1703	CSR TOROSANGUEHI	\N	\N	N	\N	\N	607	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1704	CSR FARI M'BABO	\N	\N	N	\N	\N	643	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1705	CSR GUESSIGUIE	\N	\N	N	\N	\N	583	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1706	CSR DIEVIESSOU	\N	\N	N	\N	\N	645	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1707	HG GAGNOA	\N	\N	N	\N	\N	668	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1708	DR DUOKRO	\N	\N	N	\N	\N	695	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1709	Disp. Rurale TAKISALEKRO	\N	\N	N	\N	\N	688	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1710	CSR DOUKOUYO	\N	\N	N	\N	\N	666	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1711	DR DOUGBAFLA	\N	\N	N	\N	\N	669	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1712	DR LAPO	\N	\N	N	\N	\N	628	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1713	Dispensaire GROGRO	\N	\N	N	\N	\N	692	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1714	CSR N'GANGRO'	\N	\N	N	\N	\N	684	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1715	CSU KREGBE	\N	\N	N	\N	\N	727	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1716	CSR ETICOON	\N	\N	N	\N	\N	634	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1717	CESACO OUANGOLODOUGOU	\N	\N	N	\N	\N	600	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1718	CSU TOUMODI SAKASSOU	\N	\N	N	\N	\N	701	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1719	CSU YAKASSE FEYASSE	\N	\N	N	\N	\N	705	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1720	CSR DIASSA	\N	\N	N	\N	\N	756	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1721	CSR TEZIE	\N	\N	N	\N	\N	760	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1722	CSR GOGOGUHE	\N	\N	N	\N	\N	757	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1723	CAT DIVO	\N	\N	N	\N	\N	694	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1724	CSR BOBIA	\N	\N	N	\N	\N	769	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1725	CSU BROUKRO	\N	\N	N	\N	\N	700	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1726	CSR TIEPLE	\N	\N	N	\N	\N	650	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1727	CSR VARALE	\N	\N	N	\N	\N	673	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1728	CSR KONGODEKRO	\N	\N	N	\N	\N	649	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1729	CSR OUFFOUE DIEKRO	\N	\N	N	\N	\N	693	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1730	CSR DAHIRI	\N	\N	N	\N	\N	754	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1731	CSU AMANVI	\N	\N	N	\N	\N	724	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1732	CSR KOSSEHOA	\N	\N	N	\N	\N	667	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1733	CSU TIEDO	\N	\N	N	\N	\N	715	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1734	HG ARRAH	\N	\N	N	\N	\N	725	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1735	CSR KAKPIN	\N	\N	N	\N	\N	677	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1736	CSR BANDOLE	\N	\N	N	\N	\N	680	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1737	CSR DANANGORO	\N	\N	N	\N	\N	764	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1738	CSR MONTEZO	\N	\N	N	\N	\N	786	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1739	Hopital Municipal Vridicite	\N	\N	N	\N	\N	HV	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1740	CSR DASSIOKO	\N	\N	N	\N	\N	753	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1741	CSU BAYOTA	\N	\N	N	\N	\N	842	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1742	CSR ZAKOEOUA	\N	\N	N	\N	\N	802	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1743	CSR SOUMINASSE	\N	\N	N	\N	\N	675	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1744	CSU OURAGAHIO	\N	\N	N	\N	\N	843	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1745	CSR ZAHIA	\N	\N	N	\N	\N	845	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1746	PMI BOUNA	\N	\N	N	\N	\N	867	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1747	CSU KANZRA	\N	\N	N	\N	\N	766	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1748	CSU COM ALIODAN	\N	\N	N	\N	\N	AL	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1749	CSR KRAKRO	\N	\N	N	\N	\N	866	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1750	HG TENGRELA	\N	\N	N	\N	\N	857	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1751	CSU KANAKONO	\N	\N	N	\N	\N	859	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1752	Disp. BROKOUA	\N	\N	N	\N	\N	762	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1753	DR KANANGO-KPLI	\N	\N	N	\N	\N	699	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1754	CSU SINEMATIALI	\N	\N	N	\N	\N	853	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1755	CSU BINAWA	\N	\N	N	\N	\N	834	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1756	CSR BONIANKRO	\N	\N	N	\N	\N	691	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1757	CSU OUELLE	\N	\N	N	\N	\N	736	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1758	CSU ANANDA	\N	\N	N	\N	\N	737	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1759	CSR DADIASSE	\N	\N	N	\N	\N	720	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1760	Disp. Urbain AKOUEDO	\N	\N	N	\N	\N	622	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1761	CSU CECHI	\N	\N	N	\N	\N	587	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1762	CSR BLANFLA	\N	\N	N	\N	\N	765	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1763	FSU COM ANOUMABO	\N	\N	N	\N	\N	AN	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1764	CSR MANOUFLA	\N	\N	N	\N	\N	768	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1765	CSR N'GATTAKRO'	\N	\N	N	\N	\N	738	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1766	CSR LOGOBIA	\N	\N	N	\N	\N	770	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1767	CSU GAGNOA	\N	\N	N	\N	\N	844	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1768	CHR DIVO	\N	\N	N	\N	\N	783	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1769	CSR LESSIRI	\N	\N	N	\N	\N	807	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1770	CSR N'ZECREZESSOU	\N	\N	N	\N	\N	745	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1771	CSU KOUASSI KOUASSIKRO	\N	\N	N	\N	\N	747	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1772	CHR SEGUELA	\N	\N	N	\N	\N	868	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1773	CSU NAPIE	\N	\N	N	\N	\N	848	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1774	HG YOP. ATTIE	\N	\N	N	\N	\N	YA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1775	HG YOP. ATTIE	\N	\N	N	\N	\N	YA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1776	Centre NAZAREEN	\N	\N	N	\N	\N	YZ	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1777	CSR ZAHAKRO	\N	\N	N	\N	\N	840	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1778	CSU OUPOYO	\N	\N	N	\N	\N	797	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1779	CSU MEMNI	\N	\N	N	\N	\N	788	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1780	HG ALEPE	\N	\N	N	\N	\N	790	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1781	DR M'POUTO	\N	\N	N	\N	\N	782	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1782	CSR WONSEALY	\N	\N	N	\N	\N	801	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1783	HG BUYO	\N	\N	N	\N	\N	811	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1784	CHR KORHOGO	\N	\N	N	\N	\N	846	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1785	CSR AKOBRAKRE	\N	\N	N	\N	\N	704	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1786	SSSU DIVO	\N	\N	N	\N	\N	751	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1787	CSU DEBRIMOU	\N	\N	N	\N	\N	879	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1788	CSR DANTOGO	\N	\N	N	\N	\N	778	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1789	CSR OUELOUTO	\N	\N	N	\N	\N	824	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1790	CSU M'BENGUE	\N	\N	N	\N	\N	852	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1791	DU DALOA	\N	\N	N	\N	\N	888	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1792	CSR BRINGAKRO	\N	\N	N	\N	\N	837	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1793	CSU KOFFI AMONKRO	\N	\N	N	\N	\N	749	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1794	DR BROUAGUI	\N	\N	N	\N	\N	813	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1795	CSR GRAND YAPO	\N	\N	N	\N	\N	624	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1796	CSU OGLUWAPO	\N	\N	N	\N	\N	789	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1797	Disp. St Anne BOCANDA	\N	\N	N	\N	\N	748	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1798	DR MAHINO	\N	\N	N	\N	\N	820	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1799	CSU KONDROBO	\N	\N	N	\N	\N	646	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1800	Maternite Sante pour Tous	\N	\N	N	\N	\N	773	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1801	CSU GANAONI	\N	\N	N	\N	\N	603	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1802	Disp. Cath. Notre Dame de la Prov. Daloa	\N	\N	N	\N	\N	659	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1803	CSU BUYO PLATEAU	\N	\N	N	\N	\N	812	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1804	CSR AMANGBEU	\N	\N	N	\N	\N	626	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1805	DR NEKA VILLAGE	\N	\N	N	\N	\N	822	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1806	CMS COOPAGA	\N	\N	N	\N	\N	825	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1807	CSR SRAN-BELAKRO	\N	\N	N	\N	\N	696	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1808	CSR KONGOTTI	\N	\N	N	\N	\N	739	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1809	CSR BEMADI	\N	\N	N	\N	\N	657	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1810	CSR GBEGBESSOU	\N	\N	N	\N	\N	758	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1811	CNTS	\N	\N	N	\N	\N	TS	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1812	CSR ASSIE KOUMASSI	\N	\N	N	\N	\N	731	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1813	CSU ABOISSO COMOE	\N	\N	N	\N	\N	787	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1814	CSR GUESSIHIO	\N	\N	N	\N	\N	771	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1815	CSR KOUVE	\N	\N	N	\N	\N	831	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1816	CS Don Tonino de Pullega	\N	\N	N	\N	\N	660	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1817	CSR ABOULIE	\N	\N	N	\N	\N	903	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1818	CSU ANDE	\N	\N	N	\N	\N	730	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1819	CHR MAN	\N	\N	N	\N	\N	880	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1820	CSR SAGBOYA	\N	\N	N	\N	\N	800	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1821	CSU SOUKROUGBAN	\N	\N	N	\N	\N	780	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1822	CAT KOUMASSI	\N	\N	N	\N	\N	CK	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1823	HOTEL DIEU	\N	\N	N	\N	\N	HDI	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1824	CSU COM CAMPEMENT	\N	\N	N	\N	\N	CC	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1825	CSR DJANGOBO	\N	\N	N	\N	\N	708	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1826	CSR NIGUI ASSOKO	\N	\N	N	\N	\N	632	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1827	CSR TIAGBA	\N	\N	N	\N	\N	633	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1828	CSR DIDAKAYABO	\N	\N	N	\N	\N	744	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1829	CSR EROBO	\N	\N	N	\N	\N	729	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1830	CSU IROBO	\N	\N	N	\N	\N	829	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1831	SAINT CAMILLE	\N	\N	N	\N	\N	SC	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1832	CS COM KOUMASSI DIVO	\N	\N	N	\N	\N	CSK	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1833	FSU COM ANDOKOI	\N	\N	N	\N	\N	924	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1834	CRS N'GUESSANKRO( AGNIBILEKRO)	\N	\N	N	\N	\N	714	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1835	HG TIAPOUM	\N	\N	N	\N	\N	874	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1836	Disp. Uurbain DIVO	\N	\N	N	\N	\N	784	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1837	CSR N'ZANFOUENOU	\N	\N	N	\N	\N	726	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1838	CSR AKOI N'DENOU	\N	\N	N	\N	\N	690	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1839	CSR NOUVEL OUSROU	\N	\N	N	\N	\N	914	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1840	FSU COM ADIOPODOUME	\N	\N	N	\N	\N	921	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1841	CSR BOUBOURY	\N	\N	N	\N	\N	915	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1842	CSR ABRACO	\N	\N	N	\N	\N	417	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1843	CSU COM ZOE BRUNO	\N	\N	N	\N	\N	702	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1844	CSR ABIGUI	\N	\N	N	\N	\N	741	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1845	CSU TIEMELEKRO	\N	\N	N	\N	\N	732	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1846	CSR N'GATTADOLIKRO	\N	\N	N	\N	\N	685	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1847	CSR MORONOU	\N	\N	N	\N	\N	836	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1848	Clinique Med. NANAN YAMOUSSO	\N	\N	N	\N	\N	NY	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1849	CSU COM AKLOMIABLA(KOUMASSI)	\N	\N	N	\N	\N	CK	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1850	CSR LATAHA	\N	\N	N	\N	\N	854	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1851	CSU DIKODOUGOU	\N	\N	N	\N	\N	849	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1852	CSU SIRASSO	\N	\N	N	\N	\N	850	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1853	CSR ATTIEKRO	\N	\N	N	\N	\N	710	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1854	CSR SEREBISSOU	\N	\N	N	\N	\N	733	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1855	CSR ABEHANOU	\N	\N	N	\N	\N	743	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1856	CSU LIBREVILLE MAN	\N	\N	N	\N	\N	881	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1857	PPH CHU Treichville	\N	\N	N	\N	\N	PPT	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1858	GMP	\N	\N	N	\N	\N	GP	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1859	CSU SOUNGASSOU	\N	\N	N	\N	\N	742	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1860	FSU WILLIAMSVILLE	\N	\N	N	\N	\N	FW	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1861	Gyneco CHU Treichville	\N	\N	N	\N	\N	GT	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1862	CSR NEMENE	\N	\N	N	\N	\N	652	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1863	CSR ASSALE KOUASSIKRO	\N	\N	N	\N	\N	728	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1864	CSR BENGASSOU	\N	\N	N	\N	\N	746	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1865	CSR HEREBO	\N	\N	N	\N	\N	681	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1866	CSR OUATTE	\N	\N	N	\N	\N	718	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1867	CSR FARAKO	\N	\N	N	\N	\N	676	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1868	CSR KONEDOUGOU	\N	\N	N	\N	\N	806	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1869	CSU GOUDOUKO	\N	\N	N	\N	\N	785	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1870	PMI SEGUELA	\N	\N	N	\N	\N	869	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1871	CSR ADDAH	\N	\N	N	\N	\N	827	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1872	CSR KARAKO	\N	\N	N	\N	\N	864	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1873	HDJ BOUAKE	\N	\N	N	\N	\N	HB	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1874	CSR ASSOUINDE	\N	\N	N	\N	\N	902	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1875	CSU COM AZITO	\N	\N	N	\N	\N	920	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1876	CSU DIMENDOUGOU	\N	\N	N	\N	\N	682	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1877	CSU ABOBO DOUME	\N	\N	N	\N	\N	AD	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1878	CSR BROZAN	\N	\N	N	\N	\N	759	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1879	CSR DIAMBA	\N	\N	N	\N	\N	721	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1880	CSR ABROUAMOUE	\N	\N	N	\N	\N	709	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1881	Maternite ATTECOUBE	\N	\N	N	\N	\N	MA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1882	CSU MOLONOU	\N	\N	N	\N	\N	687	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1883	DR KOSSOYO	\N	\N	N	\N	\N	817	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1884	CSR SASSAKO	\N	\N	N	\N	\N	826	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1885	CSR KOUASSI ANANGUINI	\N	\N	N	\N	\N	722	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1886	CSR SONONZO	\N	\N	N	\N	\N	776	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1887	CSU ABY	\N	\N	N	\N	\N	942	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1888	CSU COM LOKOUA	\N	\N	N	\N	\N	919	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1889	HMIF	\N	\N	N	\N	\N	MF	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1890	Polyclinique Indenie	\N	\N	N	\N	\N	PII	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1891	DAV/INHP	\N	\N	N	\N	\N	DI	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1892	DR TIBETA	\N	\N	N	\N	\N	663	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1893	CSR GUIENDE	\N	\N	N	\N	\N	716	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1894	CSU TIBETA	\N	\N	N	\N	\N	663	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1895	CSU KOUNAHIRI	\N	\N	N	\N	\N	755	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1896	CSR GNABOYA	\N	\N	N	\N	\N	814	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1897	CSR TOUADJI	\N	\N	N	\N	\N	810	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1898	CSU AVAGOU	\N	\N	N	\N	\N	828	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1899	CSR KOUASSIBLEKRO	\N	\N	N	\N	\N	671	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1900	CHR GUIGLO	\N	\N	N	\N	\N	887	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1901	SOS ABOBO	\N	\N	N	\N	\N	774	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1902	CSR ASSOMLAN	\N	\N	N	\N	\N	901	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1903	CSU ETUEBOUE	\N	\N	N	\N	\N	873	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1904	AIBEF San Pedro	\N	\N	N	\N	\N	642	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1905	CM CROIX ROUGE Attecoube	\N	\N	N	\N	\N	833	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1906	CSU KRINDJABO	\N	\N	N	\N	\N	909	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1907	Cite de l'Enfance Adjame'	\N	\N	N	\N	\N	CA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1908	DR GNAKORAGUI	\N	\N	N	\N	\N	809	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1909	CSU DAHILI	\N	\N	N	\N	\N	803	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1910	Clinique St GABRIEL	\N	\N	N	\N	\N	934	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1911	CSR EBOUE	\N	\N	N	\N	\N	943	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1912	CMS Bethesda Abengourou	\N	\N	N	\N	\N	944	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1913	Centre Maria Grazia Barbussia	\N	\N	N	\N	\N	945	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1914	Disp. Rural de Kouassi Beniekro	\N	\N	N	\N	\N	946	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1915	Disp. Urbain Dioulakro	\N	\N	N	\N	\N	947	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1916	Disp. Rural Kokonou	\N	\N	N	\N	\N	948	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1917	Disp. Rural Djangokro	\N	\N	N	\N	\N	949	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1918	CSR Zanzansou	\N	\N	N	\N	\N	950	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1919	CSU ETROKRO	\N	\N	N	\N	\N	951	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1920	CSU COM de Agoueto	\N	\N	N	\N	\N	952	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1921	CSU COM ASSOUMIN	\N	\N	N	\N	\N	953	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1922	Disp. Urbain Bloc 500	\N	\N	N	\N	\N	954	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1923	Disp. Rural de Logbakro	\N	\N	N	\N	\N	955	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1924	CSU AKOUPE ANYAMA	\N	\N	N	\N	\N	890	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1925	Dermato CHU Treichville	\N	\N	N	\N	\N	DT	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1926	CSR Vieux BADIEN	\N	\N	N	\N	\N	916	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1927	CSR ORBAF	\N	\N	N	\N	\N	917	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1928	G C M Route Akeikoi	\N	\N	N	\N	\N	781	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1929	CM Tresor	\N	\N	N	\N	\N	CM	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1930	CSU AFFIENOU	\N	\N	N	\N	\N	908	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1931	CSU ADJOUAN	\N	\N	N	\N	\N	906	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1932	CSU EBILASSOKRO	\N	\N	N	\N	\N	706	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1933	DR WALEBO	\N	\N	N	\N	\N	808	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1934	CM Bethesda	\N	\N	N	\N	\N	922	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1935	CHR ODIENNE	\N	\N	N	\N	\N	885	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1936	CHR TOUBA	\N	\N	N	\N	\N	886	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1937	CHR ABOISSO	\N	\N	N	\N	\N	870	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1938	HG MINIGNIAN	\N	\N	N	\N	\N	579	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1939	CSU Com PALMERAIE	\N	\N	N	\N	\N	CP	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1940	CM Mere Maria	\N	\N	N	\N	\N	MM	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1941	CSHKB Blockauss	\N	\N	N	\N	\N	BB	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1942	IPCI	\N	\N	N	\N	\N	IP	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1943	CMS Ste THERESE E. J	\N	\N	N	\N	\N	CMT	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1944	Polyclinique Inter. Indenie	\N	\N	N	\N	\N	929	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1945	CSU DAPEOUA	\N	\N	N	\N	\N	798	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1946	HG MAFERE	\N	\N	N	\N	\N	871	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1947	CSR MOUYASSUE	\N	\N	N	\N	\N	910	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1948	DR ZOHOURAYO	\N	\N	N	\N	\N	816	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1949	DR KAMALA	\N	\N	N	\N	\N	679	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1950	PMI ATTECOUBE	\N	\N	N	\N	\N	PA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1951	CSU BROFODOUME	\N	\N	N	\N	\N	889	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1952	Clinique Medicale ROCHE	\N	\N	N	\N	\N	CR	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1953	CSU COM KOUMASSI ZOE BRUNO	\N	\N	N	\N	\N	CZ	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1954	FSU COM VRIDI CANAL	\N	\N	N	\N	\N	FV	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1955	Centre Sante GNAGO II	\N	\N	N	\N	\N	664	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1956	DR HYRRE	\N	\N	N	\N	\N	821	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1957	Disp. Munic. M'POUTO	\N	\N	N	\N	\N	DM	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1958	CARITAS PB	\N	\N	N	\N	\N	CA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1959	Disp. Munic. M'BADON	\N	\N	N	\N	\N	DB	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1960	CM Grand Centre	\N	\N	N	\N	\N	931	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1961	CSR AGBOSSOU	\N	\N	N	\N	\N	734	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1962	HG ADJAME	\N	\N	N	\N	\N	964	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1963	Centre Med. KOUMASSI	\N	\N	N	\N	\N	CMK	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1964	CSR GNAMAGUI	\N	\N	N	\N	\N	804	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1965	Disp. Urbain COCODY	\N	\N	N	\N	\N	DUC	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1966	PMI ABOISSO	\N	\N	N	\N	\N	872	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1967	CSR ATTIGBE KOFFIKRO	\N	\N	N	\N	\N	911	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1968	G. Medical Plateau	\N	\N	N	\N	\N	GP	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1969	CSU ATTINGUE	\N	\N	N	\N	\N	891	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1970	Maternite AYENOUAN	\N	\N	N	\N	\N	905	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1971	CSR NOFOU	\N	\N	N	\N	\N	740	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1972	CMS AKWABA D. WHARF	\N	\N	N	\N	\N	CMW	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1973	HM Vridi cité	\N	\N	N	\N	\N	HMV	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1974	CSR KAKOUKRO-LIMITE	\N	\N	N	\N	\N	904	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1975	CSR BONIKRO	\N	\N	N	\N	\N	835	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1976	HM VRIDI CITE	\N	\N	N	\N	\N	HVC	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1977	CM Akouedo	\N	\N	N	\N	\N	CMA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1978	CSU COM VRIDI 3	\N	\N	N	\N	\N	CCV	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1979	HG Treichville	\N	\N	N	\N	\N	HGT	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1980	Matern. WILLIAMSVILLE	\N	\N	N	\N	\N		\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	N	\N	\N	\N	\N
1981	H.G. ADZOPE	\N	\N	N	\N	\N	HA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1982	CMS SEMPA	\N	\N	N	\N	\N	CMP	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1983	FSU 220 Lgts	\N	\N	N	\N	\N	FL	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1984	CAT Treichville	\N	\N	N	\N	\N	TC	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1985	Disp. KOTOBI	\N	\N	N	\N	\N	""	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.507549	N	Y	\N	\N	\N	\N
1987	CSU KOUASSIKRO	\N	\N	N	\N	\N	CKK	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1988	CSUC / N.D.A.	\N	\N	N	\N	\N	CN	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1989	CHR BOUAFLE	\N	\N	N	\N	\N	CB	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1990	Mat. DIOULABOUGOU	\N	\N	N	\N	\N	MD	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1991	CSU ETTROKRO	\N	\N	N	\N	\N	CE	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1992	CSU KONGOUDI	\N	\N	N	\N	\N	CK	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1993	D.F.C. BONGOUANOU	\N	\N	N	\N	\N	DFB	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1994	DU COCODY	\N	\N	N	\N	\N	DC	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1995	CSR LARABIA	\N	\N	N	\N	\N	963	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1996	FSU COM ABOBO SAGBE	\N	\N	N	\N	\N	FAS	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1997	CSU COM AGBAN	\N	\N	N	\N	\N	CCA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1998	HG MBATTO	\N	\N	N	\N	\N	530	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1999	CSR APPOUASSO	\N	\N	N	\N	\N	501	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2000	IRF ADZOPE	\N	\N	N	\N	\N	IA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2001	xxx	\N	\N	N	\N	\N	xxx	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2002	HG AKOUPE	\N	\N	N	\N	\N	HAK	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2003	CSR AKOBROKIE	\N	\N	N	\N	\N	CSA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2004	CSR SAKIARE	\N	\N	N	\N	\N	CS	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2005	HG M'BAHIAKRO	\N	\N	N	\N	\N	HB	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2006	CSU COM ASSOMIN	\N	\N	N	\N	\N	CCS	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2007	F.C. ANONKOUA KOUTE	\N	\N	N	\N	\N	65	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2008	CSU COM AGOUOTO	\N	\N	N	\N	\N	1173	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2009	CSR AKOBROKE	\N	\N	N	\N	\N	CA	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2010	HG MARCORY	\N	\N	N	\N	\N	HM	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2011	ESPECE MED. Les RUCHES	\N	\N	N	\N	\N	EMR	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2012	CSU LOCODJRO	\N	\N	N	\N	\N	CL	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2013	CSU ASSAHARA (Maternite)	\N	\N	N	\N	\N	CAS	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2014	DR M'POSSA	\N	\N	N	\N	\N	907	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2015	CSU ASSAHARA	\N	\N	N	\N	\N	735	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2016	CSR ZOHOA	\N	\N	N	\N	\N	982	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2017	CSU SANGOUINE	\N	\N	N	\N	\N	884	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2018	DR NIAHIRIO	\N	\N	N	\N	\N	981	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2019	CSU AKOUEDO	\N	\N	N	\N	\N	CAK	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2020	Clinique PROVIDENCE	\N	\N	N	\N	\N	CP	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2021	HG ATTECOUBE	\N	\N	N	\N	\N	66	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2022	CITE de l' Enfance	\N	\N	N	\N	\N	83	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2023	MAC DIMBOKRO	\N	\N	N	\N	\N	1878	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2024	CHR Pediatrie YAKRO	\N	\N	N	\N	\N	1381	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2025	FSU AFFERY	\N	\N	N	\N	\N	375	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2026	CSU BONGO	\N	\N	N	\N	\N	894	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2027	FSU COM ANONKOUA	\N	\N	N	\N	\N	65	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
1436	PROJECT RETROCI	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2012-06-20 13:28:12.485542	\N	Y	\N	\N	\N	\N
1454	MARS	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2013-04-01 11:29:51.435	\N	Y	\N	\N	\N	\N
1435	CIRBA	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2012-06-20 13:28:12.485542	\N	Y	\N	\N	\N	\N
1455	2	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2013-06-21 16:08:33.518	\N	Y	\N	\N	\N	\N
1456	CHR KORHOGO	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2013-06-24 11:30:04.695	\N	Y	\N	\N	\N	\N
1457	CHR ABENGOUROU	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2013-06-26 14:31:26.558	\N	Y	\N	\N	\N	\N
1458	TEST	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2013-06-27 12:25:34.216	\N	Y	\N	\N	\N	\N
1459	LABODOUGOU	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2013-06-27 12:26:22.476	\N	Y	\N	\N	\N	\N
1460	YAM	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2013-07-30 12:22:01.258	\N	Y	\N	\N	\N	\N
1986	Disp. STE A. BOCANDA	\N	\N	N	\N	\N	""	\N	\N	MN	\N	\N	\N	2020-01-22 21:46:41.645237	N	Y	\N	\N	\N	\N
2500	CEDRES	\N	\N	N	\N	\N	""	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:42.06253	\N	Y	\N	\N	\N	\N
\.


--
-- Data for Name: organization_address; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.organization_address (organization_id, address_part_id, type, value) FROM stdin;
\.


--
-- Data for Name: organization_contact; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.organization_contact (id, organization_id, person_id, "position") FROM stdin;
\.


--
-- Name: organization_contact_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.organization_contact_seq', 23, true);


--
-- Data for Name: organization_organization_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.organization_organization_type (org_id, org_type_id) FROM stdin;
7	1
1329	8
1330	7
1331	7
1332	7
1333	7
1334	7
1335	7
1336	8
1337	7
1338	7
1339	7
1340	7
1341	7
1342	8
1343	7
1344	7
1345	8
1346	7
1347	7
1348	7
1349	8
1350	7
1351	7
1352	8
1353	7
1354	7
1355	7
1356	7
1357	8
1358	7
1359	7
1360	7
1361	7
1362	7
1363	7
1364	7
1365	7
1366	7
1367	8
1368	7
1369	7
1370	7
1371	7
1372	7
1373	7
1374	7
1375	7
1376	7
1377	7
1378	8
1379	7
1380	7
1381	7
1382	8
1383	7
1384	7
1385	7
1386	7
1387	8
1388	7
1389	7
1390	7
1391	7
1392	7
1393	7
1394	8
1395	7
1396	7
1397	7
1398	7
1399	7
1400	7
1401	8
1402	7
1403	7
1404	7
1405	8
1406	7
1407	7
1408	7
1409	7
1410	8
1411	7
1412	7
1413	8
1414	7
1415	7
1416	7
1417	8
1418	7
1419	7
1420	7
1421	7
1422	7
1423	8
1424	7
1425	7
1426	7
1427	8
1428	7
1429	7
1430	8
1431	7
1432	7
1433	7
1434	7
1435	6
1436	6
1447	5
1448	5
1449	5
1450	5
1451	5
1452	5
1454	5
1455	5
1456	5
1457	5
1458	5
1459	5
1460	5
760	20
761	20
762	20
763	20
764	20
765	20
766	20
767	20
768	20
769	20
770	20
771	20
772	21
773	21
774	21
775	21
776	21
777	21
778	21
779	21
780	21
781	21
782	21
783	21
784	21
785	21
786	21
787	21
788	21
789	21
790	21
791	21
792	21
793	21
794	21
795	21
796	21
797	21
798	21
799	21
800	21
801	21
802	21
803	21
804	21
805	21
806	21
807	21
808	21
809	21
810	21
811	21
812	21
813	21
814	21
815	21
816	21
817	20
817	21
818	21
819	21
820	21
821	21
822	21
823	21
824	20
824	21
825	21
826	21
827	20
827	21
828	21
829	21
830	20
830	21
831	21
832	21
833	21
834	21
835	21
836	21
837	20
837	21
838	20
838	21
839	20
839	21
840	20
840	21
841	21
842	21
843	21
844	21
845	21
846	21
847	21
848	21
849	21
850	21
851	21
852	21
853	21
854	21
855	21
856	21
857	21
858	20
858	21
859	20
859	21
860	21
861	22
862	22
863	22
864	22
865	22
866	22
867	22
868	22
869	22
870	22
871	22
872	20
872	22
873	22
874	22
875	20
875	22
876	22
877	22
878	22
879	22
880	22
881	22
882	20
882	22
883	22
884	20
884	22
885	22
886	22
887	20
887	22
888	20
888	22
889	22
890	22
891	22
892	22
893	22
894	22
895	22
896	22
897	22
898	22
899	22
900	22
901	22
902	22
903	22
904	20
904	22
905	20
905	22
906	20
906	22
907	22
908	22
909	22
910	20
910	22
911	20
911	22
912	22
913	22
914	22
915	22
916	22
917	22
918	22
919	22
920	22
921	22
922	22
923	22
924	22
925	22
926	22
927	22
928	22
929	22
930	22
931	22
932	22
933	22
934	22
935	22
936	22
937	22
938	22
939	22
940	22
941	20
941	22
942	22
943	20
943	22
944	20
944	22
945	22
946	22
947	20
947	22
948	20
948	22
949	22
950	22
951	22
952	22
953	22
954	22
955	22
956	22
957	22
958	22
959	20
959	22
960	22
961	22
962	20
962	22
963	20
963	22
964	20
964	22
965	20
965	22
966	22
967	22
968	22
969	22
970	22
971	22
972	20
972	22
973	20
973	22
974	22
975	20
975	22
976	22
977	22
978	22
979	20
979	22
980	22
981	20
981	22
982	22
983	22
984	22
985	22
986	22
987	22
988	22
989	22
990	22
991	22
992	22
993	22
994	22
995	22
996	22
997	22
998	22
999	22
1000	23
1001	23
1002	24
1003	24
1004	24
1005	24
1006	24
1007	20
1007	24
1008	24
1009	24
1010	20
1010	24
1011	24
1012	24
1013	24
1014	24
1015	24
1016	24
1017	24
1018	24
1019	24
1020	24
1021	24
1022	24
1023	24
1024	24
1025	24
1026	24
1027	24
1028	24
1029	24
1030	24
1031	24
1032	24
1033	24
1034	24
1035	24
1036	24
1037	24
1038	24
1039	24
1040	24
1041	24
1042	24
1043	24
1044	24
1045	24
1046	24
1047	24
1048	24
1049	24
1050	24
1051	24
1052	24
1053	24
1054	24
1055	24
1056	24
1057	24
1058	24
1059	24
1060	24
1061	24
1062	24
1063	24
1064	24
1065	24
1066	24
1067	24
1068	24
1069	24
1070	24
1071	24
1072	24
1073	24
1074	24
1075	24
1076	24
1077	24
1078	24
1079	24
1080	24
1081	24
1082	24
1083	24
1084	24
1085	24
1086	24
1087	24
1088	24
1089	24
1090	24
1091	24
1092	24
1093	24
1094	24
1095	24
1096	24
1097	24
1098	24
1099	24
1100	24
1101	24
1102	24
1103	24
1104	25
1105	25
1106	25
1107	25
1108	25
1109	25
1110	25
1111	25
1112	25
1113	25
1114	25
1115	25
1116	25
1117	25
1118	25
1119	25
1120	25
1121	25
1122	25
1123	25
1124	20
1124	25
1125	26
1126	20
1126	26
1127	26
1129	26
1132	22
1133	20
1134	26
1135	25
1136	23
1137	20
1137	25
1138	20
1139	20
1140	22
1141	23
1142	22
1143	22
1144	22
1145	20
1145	22
1146	20
1146	22
1147	20
1147	22
1148	22
1149	20
1149	22
1150	22
1151	20
1151	22
1152	20
1152	22
1153	22
1154	22
1155	22
1156	22
1157	22
1158	20
1158	22
1159	20
1159	22
1160	20
1160	22
1161	20
1161	22
1162	20
1162	22
1163	24
1164	24
1165	24
1166	24
1167	22
1168	22
1169	22
1170	22
1171	22
1172	22
1173	22
1174	22
1175	22
1176	22
1177	22
1178	22
1179	22
1180	22
1181	22
1182	22
1183	22
1184	22
1185	22
1186	22
1187	22
1188	22
1189	22
1190	22
1191	22
1192	22
1193	22
1194	22
1195	22
1196	22
1197	22
1198	22
1199	24
1200	22
1201	22
1202	22
1203	22
1204	22
1205	22
1206	22
1207	22
1208	22
1209	22
1210	20
1210	22
1211	22
1212	22
1213	22
1214	22
1215	22
1216	22
1217	22
1218	22
1219	22
1220	22
1221	22
1222	22
1223	22
1224	22
1225	22
1226	23
1227	23
1228	23
1229	23
1230	22
1231	23
1232	23
1233	23
1234	23
1235	20
1235	22
1236	23
1237	23
1238	23
1239	23
1240	23
1241	22
1242	22
1243	23
1244	23
1245	22
1246	23
1247	23
1248	23
1249	22
1250	23
1251	23
1252	23
1253	23
1254	23
1255	23
1256	23
1257	20
1258	23
1259	23
1260	22
1261	24
1262	22
1263	23
1264	23
1265	23
1266	23
1267	22
1268	22
1269	23
1270	23
1271	23
1272	20
1272	22
1273	23
1274	23
1275	22
1276	22
1277	22
1278	22
1279	22
1280	21
1281	21
1282	21
1283	21
1284	22
1285	22
1286	22
1287	22
1288	22
1289	22
1290	22
1291	22
1292	22
1293	22
1294	22
1295	22
1296	22
1297	22
1298	22
1299	22
1300	20
1300	22
1301	22
1302	20
1302	22
1303	22
1304	22
1305	22
1306	22
1307	22
1308	22
1309	22
1310	22
1311	22
1312	22
1313	22
1314	22
1315	22
1316	22
1317	22
1318	20
1318	22
1319	22
1320	22
1321	22
1322	22
1323	22
1324	22
1325	22
1326	22
1327	22
1328	22
1600	22
1601	22
1602	22
1603	22
1604	20
1604	22
1605	23
1606	23
1607	23
1608	23
1609	23
1610	23
1611	23
1612	23
1613	23
1614	23
1615	23
1616	23
1617	23
1618	20
1618	22
1619	20
1619	22
1620	20
1621	22
1622	20
1622	26
1623	20
1624	20
1624	25
1625	21
1625	22
1625	23
1625	24
1626	24
1627	22
1628	21
1629	21
1630	21
1631	21
1632	21
1633	21
1634	20
1635	21
1636	22
1637	21
1638	21
1639	21
1640	21
1641	21
1642	21
1643	21
1644	21
1645	21
1646	21
1647	21
1648	21
1649	21
1650	23
1651	21
1652	21
1652	22
1652	23
1652	24
1653	21
1654	21
1654	22
1654	23
1654	24
1655	22
1656	21
1656	22
1656	23
1656	24
1657	23
1658	21
1659	23
1660	21
1660	22
1660	23
1660	24
1661	21
1661	22
1661	23
1661	24
1662	22
1663	23
1664	23
1665	23
1666	23
1667	23
1668	20
1669	20
1670	21
1671	22
1672	23
1673	23
1674	23
1675	23
1676	23
1677	23
1678	23
1679	23
1680	23
1681	23
1682	23
1683	23
1684	20
1685	23
1686	23
1687	23
1688	23
1689	23
1690	23
1691	23
1692	23
1693	23
1694	22
1695	23
1696	22
1697	23
1698	23
1699	21
1700	23
1701	23
1702	20
1703	23
1704	23
1705	23
1706	23
1707	23
1708	23
1709	23
1710	20
1710	23
1711	23
1712	23
1713	23
1714	22
1715	20
1715	23
1716	22
1717	23
1718	22
1719	23
1720	23
1721	23
1722	23
1723	24
1724	23
1725	23
1726	23
1727	23
1728	23
1729	22
1730	24
1731	23
1732	23
1733	23
1734	20
1734	23
1735	23
1736	23
1737	23
1738	23
1739	20
1740	23
1741	23
1742	23
1743	23
1744	23
1745	23
1746	23
1747	23
1748	20
1749	23
1750	23
1751	23
1752	23
1753	23
1754	23
1755	22
1756	22
1757	20
1757	23
1758	20
1758	23
1759	23
1760	23
1761	23
1762	20
1763	23
1764	23
1765	23
1766	23
1767	23
1768	23
1769	23
1770	23
1771	23
1772	23
1773	22
1774	20
1775	20
1776	22
1777	22
1778	24
1779	24
1780	24
1781	23
1782	23
1783	23
1784	22
1785	24
1786	23
1787	23
1788	23
1789	23
1790	23
1791	22
1792	20
1793	23
1794	23
1795	23
1796	23
1797	23
1798	23
1799	23
1800	23
1801	23
1802	23
1803	23
1804	24
1805	23
1806	23
1807	22
1808	23
1809	23
1810	23
1811	20
1812	22
1813	23
1814	23
1815	23
1816	23
1817	23
1818	20
1818	23
1819	23
1820	23
1821	23
1822	20
1823	20
1824	20
1825	23
1826	23
1827	23
1828	23
1829	22
1830	23
1831	20
1832	20
1833	21
1834	23
1835	23
1836	24
1837	23
1838	22
1839	21
1840	21
1841	21
1842	23
1843	20
1843	22
1844	23
1845	20
1845	23
1846	22
1847	20
1847	22
1848	20
1849	20
1850	23
1851	23
1852	23
1853	23
1854	23
1855	23
1856	21
1857	20
1858	20
1859	23
1860	20
1861	20
1862	23
1863	23
1864	23
1865	23
1866	23
1867	23
1868	23
1869	24
1870	23
1871	23
1872	23
1873	20
1874	23
1875	23
1876	23
1877	20
1878	23
1879	23
1880	23
1881	20
1882	22
1883	23
1884	23
1885	23
1886	23
1887	23
1888	23
1889	20
1890	20
1891	20
1892	20
1893	23
1894	23
1895	23
1896	23
1897	23
1898	23
1899	23
1900	23
1901	23
1902	23
1903	23
1904	23
1905	23
1906	23
1907	20
1908	23
1909	23
1910	20
1910	23
1911	23
1912	23
1913	23
1914	23
1915	23
1916	23
1917	23
1918	23
1919	23
1920	23
1921	23
1922	23
1923	23
1924	22
1925	20
1926	23
1927	23
1928	23
1929	20
1930	23
1931	23
1932	23
1933	23
1934	23
1935	21
1936	21
1937	23
1938	21
1939	20
1940	20
1941	20
1942	20
1943	20
1944	23
1945	23
1946	23
1947	23
1948	23
1949	23
1950	20
1951	23
1952	20
1953	20
1954	20
1955	23
1956	23
1957	20
1958	20
1959	20
1960	23
1961	23
1962	20
1962	23
1963	20
1964	23
1965	20
1966	23
1967	23
1968	20
1969	23
1970	23
1971	23
1972	20
1973	20
1974	23
1975	23
1976	20
1977	20
1978	20
1979	20
1980	20
1981	20
1982	20
1983	20
1984	20
1985	20
1986	20
1987	20
1988	20
1989	20
1990	20
1991	20
1992	20
1993	20
1994	20
1995	23
1996	20
1997	20
1998	20
1998	22
1999	20
1999	23
2000	20
2001	20
2002	20
2003	20
2004	20
2005	20
2006	20
2007	20
2008	20
2009	20
2010	20
2011	20
2012	20
2013	20
2014	20
2014	23
2015	23
2016	23
2017	23
2018	23
2019	20
2020	20
2021	20
2022	20
2023	20
2024	20
2025	20
2026	23
2027	20
759	5
760	5
761	5
762	5
763	5
764	5
765	5
766	5
767	5
768	5
769	5
770	5
771	5
772	5
773	5
774	5
775	5
776	5
777	5
778	5
779	5
780	5
781	5
782	5
783	5
784	5
785	5
786	5
787	5
788	5
789	5
790	5
791	5
792	5
793	5
794	5
795	5
796	5
797	5
798	5
799	5
800	5
801	5
802	5
803	5
804	5
805	5
806	5
807	5
808	5
809	5
810	5
811	5
812	5
813	5
814	5
815	5
816	5
817	5
818	5
819	5
820	5
821	5
822	5
823	5
824	5
825	5
826	5
827	5
828	5
829	5
830	5
831	5
832	5
833	5
834	5
835	5
836	5
837	5
838	5
839	5
840	5
841	5
842	5
843	5
844	5
845	5
846	5
847	5
848	5
849	5
850	5
851	5
852	5
853	5
854	5
855	5
856	5
857	5
858	5
859	5
860	5
861	5
862	5
863	5
864	5
865	5
866	5
867	5
868	5
869	5
870	5
871	5
872	5
873	5
874	5
875	5
876	5
877	5
878	5
879	5
880	5
881	5
882	5
883	5
884	5
885	5
886	5
887	5
888	5
889	5
890	5
891	5
892	5
893	5
894	5
895	5
896	5
897	5
898	5
899	5
900	5
901	5
902	5
903	5
904	5
905	5
906	5
907	5
908	5
909	5
910	5
911	5
912	5
913	5
914	5
915	5
916	5
917	5
918	5
919	5
920	5
921	5
922	5
923	5
924	5
925	5
926	5
927	5
928	5
929	5
930	5
931	5
932	5
933	5
934	5
935	5
936	5
937	5
938	5
939	5
940	5
941	5
942	5
943	5
944	5
945	5
946	5
947	5
948	5
949	5
950	5
951	5
952	5
953	5
954	5
955	5
956	5
957	5
958	5
959	5
960	5
961	5
962	5
963	5
964	5
965	5
966	5
967	5
968	5
969	5
970	5
971	5
972	5
973	5
974	5
975	5
976	5
977	5
978	5
979	5
980	5
981	5
982	5
983	5
984	5
985	5
986	5
987	5
988	5
989	5
990	5
991	5
992	5
993	5
994	5
995	5
996	5
997	5
998	5
999	5
1000	5
1001	5
1002	5
1003	5
1004	5
1005	5
1006	5
1007	5
1008	5
1009	5
1010	5
1011	5
1012	5
1013	5
1014	5
1015	5
1016	5
1017	5
1018	5
1019	5
1020	5
1021	5
1022	5
1023	5
1024	5
1025	5
1026	5
1027	5
1028	5
1029	5
1030	5
1031	5
1032	5
1033	5
1034	5
1035	5
1036	5
1037	5
1038	5
1039	5
1040	5
1041	5
1042	5
1043	5
1044	5
1045	5
1046	5
1047	5
1048	5
1049	5
1050	5
1051	5
1052	5
1053	5
1054	5
1055	5
1056	5
1057	5
1058	5
1059	5
1060	5
1061	5
1062	5
1063	5
1064	5
1065	5
1066	5
1067	5
1068	5
1069	5
1070	5
1071	5
1072	5
1073	5
1074	5
1075	5
1076	5
1077	5
1078	5
1079	5
1080	5
1081	5
1082	5
1083	5
1084	5
1085	5
1086	5
1087	5
1088	5
1089	5
1090	5
1091	5
1092	5
1093	5
1094	5
1095	5
1096	5
1097	5
1098	5
1099	5
1100	5
1101	5
1102	5
1103	5
1104	5
1105	5
1106	5
1107	5
1108	5
1109	5
1110	5
1111	5
1112	5
1113	5
1114	5
1115	5
1116	5
1117	5
1118	5
1119	5
1120	5
1121	5
1122	5
1123	5
1124	5
1125	5
1126	5
1127	5
1128	5
1129	5
1132	5
1133	5
1134	5
1135	5
1136	5
1137	5
1138	5
1139	5
1140	5
1141	5
1142	5
1143	5
1144	5
1145	5
1146	5
1147	5
1148	5
1149	5
1150	5
1151	5
1152	5
1153	5
1154	5
1155	5
1156	5
1157	5
1158	5
1159	5
1160	5
1161	5
1162	5
1163	5
1164	5
1165	5
1166	5
1167	5
1168	5
1169	5
1170	5
1171	5
1172	5
1173	5
1174	5
1175	5
1176	5
1177	5
1178	5
1179	5
1180	5
1181	5
1182	5
1183	5
1184	5
1185	5
1186	5
1187	5
1188	5
1189	5
1190	5
1191	5
1192	5
1193	5
1194	5
1195	5
1196	5
1197	5
1198	5
1199	5
1200	5
1201	5
1202	5
1203	5
1204	5
1205	5
1206	5
1207	5
1208	5
1209	5
1210	5
1211	5
1212	5
1213	5
1214	5
1215	5
1216	5
1217	5
1218	5
1219	5
1220	5
1221	5
1222	5
1223	5
1224	5
1225	5
1226	5
1227	5
1228	5
1229	5
1230	5
1231	5
1232	5
1233	5
1234	5
1235	5
1236	5
1237	5
1238	5
1239	5
1240	5
1241	5
1242	5
1243	5
1244	5
1245	5
1246	5
1247	5
1248	5
1249	5
1250	5
1251	5
1252	5
1253	5
1254	5
1255	5
1256	5
1257	5
1258	5
1259	5
1260	5
1261	5
1262	5
1263	5
1264	5
1265	5
1266	5
1267	5
1268	5
1269	5
1270	5
1271	5
1272	5
1273	5
1274	5
1275	5
1276	5
1277	5
1278	5
1279	5
1280	5
1281	5
1282	5
1283	5
1284	5
1285	5
1286	5
1287	5
1288	5
1289	5
1290	5
1291	5
1292	5
1293	5
1294	5
1295	5
1296	5
1297	5
1298	5
1299	5
1300	5
1301	5
1302	5
1303	5
1304	5
1305	5
1306	5
1307	5
1308	5
1309	5
1310	5
1311	5
1312	5
1313	5
1314	5
1315	5
1316	5
1317	5
1318	5
1319	5
1320	5
1321	5
1322	5
1323	5
1324	5
1325	5
1326	5
1327	5
1328	5
1600	5
1601	5
1602	5
1603	5
1604	5
1605	5
1606	5
1607	5
1608	5
1609	5
1610	5
1611	5
1612	5
1613	5
1614	5
1615	5
1616	5
1617	5
1618	5
1619	5
1620	5
1621	5
1622	5
1623	5
1624	5
1625	5
1626	5
1627	5
1628	5
1629	5
1630	5
1631	5
1632	5
1633	5
1634	5
1635	5
1636	5
1637	5
1638	5
1639	5
1640	5
1641	5
1642	5
1643	5
1644	5
1645	5
1646	5
1647	5
1648	5
1649	5
1650	5
1651	5
1652	5
1653	5
1654	5
1655	5
1656	5
1657	5
1658	5
1659	5
1660	5
1661	5
1662	5
1663	5
1664	5
1665	5
1666	5
1667	5
1668	5
1669	5
1670	5
1671	5
1672	5
1673	5
1674	5
1675	5
1676	5
1677	5
1678	5
1679	5
1680	5
1681	5
1682	5
1683	5
1684	5
1685	5
1686	5
1687	5
1688	5
1689	5
1690	5
1691	5
1692	5
1693	5
1694	5
1695	5
1696	5
1697	5
1698	5
1699	5
1700	5
1701	5
1702	5
1703	5
1704	5
1705	5
1706	5
1707	5
1708	5
1709	5
1710	5
1711	5
1712	5
1713	5
1714	5
1715	5
1716	5
1717	5
1718	5
1719	5
1720	5
1721	5
1722	5
1723	5
1724	5
1725	5
1726	5
1727	5
1728	5
1729	5
1730	5
1731	5
1732	5
1733	5
1734	5
1735	5
1736	5
1737	5
1738	5
1739	5
1740	5
1741	5
1742	5
1743	5
1744	5
1745	5
1746	5
1747	5
1748	5
1749	5
1750	5
1751	5
1752	5
1753	5
1754	5
1755	5
1756	5
1757	5
1758	5
1759	5
1760	5
1761	5
1762	5
1763	5
1764	5
1765	5
1766	5
1767	5
1768	5
1769	5
1770	5
1771	5
1772	5
1773	5
1774	5
1775	5
1776	5
1777	5
1778	5
1779	5
1780	5
1781	5
1782	5
1783	5
1784	5
1785	5
1786	5
1787	5
1788	5
1789	5
1790	5
1791	5
1792	5
1793	5
1794	5
1795	5
1796	5
1797	5
1798	5
1799	5
1800	5
1801	5
1802	5
1803	5
1804	5
1805	5
1806	5
1807	5
1808	5
1809	5
1810	5
1811	5
1812	5
1813	5
1814	5
1815	5
1816	5
1817	5
1818	5
1819	5
1820	5
1821	5
1822	5
1823	5
1824	5
1825	5
1826	5
1827	5
1828	5
1829	5
1830	5
1831	5
1832	5
1833	5
1834	5
1835	5
1836	5
1837	5
1838	5
1839	5
1840	5
1841	5
1842	5
1843	5
1844	5
1845	5
1846	5
1847	5
1848	5
1849	5
1850	5
1851	5
1852	5
1853	5
1854	5
1855	5
1856	5
1857	5
1858	5
1859	5
1860	5
1861	5
1862	5
1863	5
1864	5
1865	5
1866	5
1867	5
1868	5
1869	5
1870	5
1871	5
1872	5
1873	5
1874	5
1875	5
1876	5
1877	5
1878	5
1879	5
1880	5
1881	5
1882	5
1883	5
1884	5
1885	5
1886	5
1887	5
1888	5
1889	5
1890	5
1891	5
1892	5
1893	5
1894	5
1895	5
1896	5
1897	5
1898	5
1899	5
1900	5
1901	5
1902	5
1903	5
1904	5
1905	5
1906	5
1907	5
1908	5
1909	5
1910	5
1911	5
1912	5
1913	5
1914	5
1915	5
1916	5
1917	5
1918	5
1919	5
1920	5
1921	5
1922	5
1923	5
1924	5
1925	5
1926	5
1927	5
1928	5
1929	5
1930	5
1931	5
1932	5
1933	5
1934	5
1935	5
1936	5
1937	5
1938	5
1939	5
1940	5
1941	5
1942	5
1943	5
1944	5
1945	5
1946	5
1947	5
1948	5
1949	5
1950	5
1951	5
1952	5
1953	5
1954	5
1955	5
1956	5
1957	5
1958	5
1959	5
1960	5
1961	5
1962	5
1963	5
1964	5
1965	5
1966	5
1967	5
1968	5
1969	5
1970	5
1971	5
1972	5
1973	5
1974	5
1975	5
1976	5
1977	5
1978	5
1979	5
1980	5
1981	5
1982	5
1983	5
1984	5
1985	5
1986	5
1987	5
1988	5
1989	5
1990	5
1991	5
1992	5
1993	5
1994	5
1995	5
1996	5
1997	5
1998	5
1999	5
2000	5
2001	5
2002	5
2003	5
2004	5
2005	5
2006	5
2007	5
2008	5
2009	5
2010	5
2011	5
2012	5
2013	5
2014	5
2015	5
2016	5
2017	5
2018	5
2019	5
2020	5
2021	5
2022	5
2023	5
2024	5
2025	5
2026	5
2027	5
2500	6
\.


--
-- Name: organization_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.organization_seq', 2500, true);


--
-- Data for Name: organization_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.organization_type (id, short_name, description, name_display_key, lastupdated) FROM stdin;
1	TestKitVender	Organization selling HIV test kits	db.organization.type.name.testkit	2009-12-17 12:07:12.477554
5	referring clinic	Name of org who can order lab tests	\N	2011-05-31 17:21:14.17387
6	referralLab	An organization to which samples may be sent	orgainzation.type.referral.lab	2011-05-31 17:21:14.17387
7	Health District	District in region hospital is located	organization.type.healt.region	2012-06-05 16:56:26.981998
8	Health Region	Region hospital is located	organization.type.healt.region	2012-06-05 16:56:26.981998
9	patient referral	Clinic which referred the lab	organization.type.patient.referral	2012-06-11 09:33:47.020679
20	ARV Service Loc	Côte d'Ivoire ARV Study Service Locations	org_type.ARVSites.name	2020-01-22 21:46:41.658497
25	RTN HIV Service Loc	Côte d'Ivoire Routine HIV Tests Service Locations	org_type.RTNSites.name	2020-01-22 21:46:41.658497
26	RTN HIV Hospitals	Côte d'Ivoire Routine HIV Tests Hospitals	org_type.RTNHsptl.name	2020-01-22 21:46:41.658497
21	EID ACONDA-VS CI	ACONDA-VS Côte d'Ivoire	org_type.ACONDA.name	2020-01-22 21:46:41.658497
22	EID EGPAF	Elizabeth Glaser Pediatric AIDS Foundation	org_type.EGPAF.name	2020-01-22 21:46:41.658497
23	EID ESTHER	Ensemble pour une Solidarité Thérapeutique Hosp. En Réseau	org_type.ESTHER.name	2020-01-22 21:46:41.658497
24	EID ICAP	The Internat. Center for AIDS Care and Treatment Programs	org_type.ICAP.name	2020-01-22 21:46:41.658497
27	EID SEV-CI	SEV-CI	org_type.SEVCI.name	2020-01-22 21:46:41.658497
28	EID ARIEL	Ariel Glaser Foundation	org_type.ARIEL.name	2020-01-22 21:46:41.658497
\.


--
-- Name: organization_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.organization_type_seq', 9, true);


--
-- Data for Name: package_1; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.package_1 (id) FROM stdin;
\.


--
-- Data for Name: panel; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.panel (id, name, description, lastupdated, sort_order, is_active, name_localization_id) FROM stdin;
1	Bilan Biochimique	Bilan Biochimique	2012-10-24 11:58:25.204586	10	Y	132
2	NFS	NFS	2012-10-24 11:58:25.204586	20	Y	133
4	Serologie VIH	Serologie VIH	2012-10-24 11:58:25.204586	40	Y	134
3	Typage lymphocytaire	Typage lymphocytaire	2012-10-24 11:58:25.204586	30	Y	135
\.


--
-- Data for Name: panel_item; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.panel_item (id, panel_id, sort_order, test_local_abbrev, method_name, lastupdated, test_name, test_id) FROM stdin;
1	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	1
2	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	2
3	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	3
4	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	4
5	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	5
6	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	6
7	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	7
8	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	8
9	1	\N	\N	\N	2012-10-24 11:58:25.337313	\N	9
10	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	13
11	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	14
12	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	15
13	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	16
14	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	17
15	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	18
16	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	19
17	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	20
18	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	21
19	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	22
20	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	23
21	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	24
22	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	25
23	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	26
24	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	27
25	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	28
26	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	29
27	2	\N	\N	\N	2012-10-24 11:58:25.337313	\N	30
28	3	\N	\N	\N	2012-10-24 11:58:25.337313	\N	34
29	3	\N	\N	\N	2012-10-24 11:58:25.337313	\N	35
30	4	\N	\N	\N	2012-10-24 11:58:25.337313	\N	39
31	4	\N	\N	\N	2012-10-24 11:58:25.337313	\N	40
\.


--
-- Name: panel_item_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.panel_item_seq', 31, true);


--
-- Name: panel_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.panel_seq', 4, true);


--
-- Data for Name: patient; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.patient (id, person_id, race, gender, birth_date, epi_first_name, epi_middle_name, epi_last_name, birth_time, death_date, national_id, ethnicity, school_attend, medicare_id, medicaid_id, birth_place, lastupdated, external_id, chart_number, entered_birth_date) FROM stdin;
\.


--
-- Data for Name: patient_identity; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.patient_identity (id, identity_type_id, patient_id, identity_data, lastupdated) FROM stdin;
\.


--
-- Name: patient_identity_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.patient_identity_seq', 1, false);


--
-- Data for Name: patient_identity_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.patient_identity_type (id, identity_type, description, lastupdated) FROM stdin;
2	ST	ST Number	2008-11-05 10:36:39.615
3	AKA	Also known as name	2008-11-05 10:36:39.615
4	MOTHER	Mothers name	2008-11-05 10:36:39.615
1	NATIONAL	National ID	2008-11-05 10:36:39.615
5	INSURANCE	Primary insurance number	\N
6	OCCUPATION	patients occupation	\N
9	SUBJECT	Subject Number	2010-01-06 12:56:16.166813
8	ORG_SITE	Organization Site	2010-01-06 12:56:39.622399
11	MOTHERS_INITIAL	Initial of mothers first name	2010-03-15 13:15:08.22301
14	GUID	A GUID for the patient.  May be generated by a master patient index	2011-03-29 15:37:16.635053
15	EDUCATION	Patients education level	2012-06-14 12:12:44.334473
16	MARITIAL	Patients maritial status	2012-06-14 12:12:44.334473
17	NATIONALITY	Patients nationality	2012-06-14 12:12:44.334473
18	OTHER NATIONALITY	Named nationality if OTHER is selected	2012-06-14 12:12:44.334473
19	HEALTH DISTRICT	Patients health district	2012-06-14 12:12:44.334473
20	HEALTH REGION	Patients health region	2012-06-14 12:12:44.334473
21	OB_NUMBER	Obstetrics ID number	2013-04-29 10:45:02.158274
22	PC_NUMBER	Primary care ID number	2013-04-29 10:45:02.158274
\.


--
-- Name: patient_identity_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.patient_identity_type_seq', 22, true);


--
-- Name: patient_occupation_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.patient_occupation_seq', 1, false);


--
-- Data for Name: patient_patient_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.patient_patient_type (id, patient_type_id, patient_id, lastupdated) FROM stdin;
\.


--
-- Name: patient_patient_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.patient_patient_type_seq', 1, false);


--
-- Name: patient_relation_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.patient_relation_seq', 1, false);


--
-- Data for Name: patient_relations; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.patient_relations (id, pat_id_source, pat_id, relation) FROM stdin;
\.


--
-- Name: patient_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.patient_seq', 1, false);


--
-- Data for Name: patient_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.patient_type (id, type, description, lastupdated) FROM stdin;
1	R	Referré	2009-07-09 13:06:10.215545
2	E	Patient Externe	2009-07-09 13:06:10.215545
3	H	Hospitalisé	2009-07-09 13:06:10.215545
4	U	Urgences	2009-07-09 13:06:10.215545
5	P	Patient Privé	2009-07-09 13:06:10.215545
\.


--
-- Name: patient_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.patient_type_seq', 20, true);


--
-- Data for Name: payment_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.payment_type (id, type, description) FROM stdin;
\.


--
-- Name: payment_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.payment_type_seq', 1, false);


--
-- Data for Name: person; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.person (id, last_name, first_name, middle_name, multiple_unit, street_address, city, state, zip_code, country, work_phone, home_phone, cell_phone, fax, email, lastupdated, primary_phone) FROM stdin;
\.


--
-- Data for Name: person_address; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.person_address (person_id, address_part_id, type, value) FROM stdin;
\.


--
-- Name: person_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.person_seq', 1, false);


--
-- Data for Name: program; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.program (id, code, name, lastupdated) FROM stdin;
\.


--
-- Name: program_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.program_seq', 1, false);


--
-- Data for Name: project; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.project (id, name, sys_user_id, description, started_date, completed_date, is_active, reference_to, program_code, lastupdated, scriptlet_id, local_abbrev, display_key) FROM stdin;
1	Testing	\N	Sample sent to lab for testing	\N	\N	1	\N	\N	2011-02-15 15:28:32.157041	\N	\N	database.project.testing
2	Confirmation	\N	Sample sent to lab for confirmation of result	\N	\N	1	\N	\N	2011-02-15 15:28:32.157041	\N	\N	database.project.confirmation
3	Quality Control	\N	Sample is part of quality control	\N	\N	1	\N	\N	2011-02-15 15:28:32.157041	\N	\N	database.project.quality_control
4	EEQ	\N	Sample is part of external quality control	\N	\N	1	\N	\N	2011-02-15 15:28:32.157041	\N	\N	database.project.eeq
21	Indeterminate Results	\N	Indeterminate Results	\N	\N	Y	\N	LIND	2020-01-22 21:46:41.434205	\N	\N	project.IndeterminateStudy.name
22	Cell Sequencing	\N	Cell Sequencing	\N	\N	Y	\N	LSEQ	2020-01-22 21:46:41.434205	\N	\N	project.SequencingStudy.name
23	Special Request	\N	Special Requests	\N	\N	Y	\N	LSPE	2020-01-22 21:46:41.434205	\N	\N	project.SpecialRequestStudy.name
24	Antiretroviral Study	\N	Antiretroviral Treatment Study	\N	\N	Y	\N	LARC	2020-01-22 21:46:41.434205	\N	\N	project.ARVStudy.name
25	Early Infant Diagnosis for HIV Study	\N	Early Infant Diagnosis for HIV Study	\N	\N	Y	\N	LDBS	2020-01-22 21:46:41.434205	\N	\N	project.EIDStudy.name
26	Routine HIV Testing	\N	Routine HIV Tests	\N	\N	Y	\N	LRTN	2020-01-22 21:46:41.434205	\N	\N	project.RTNStudy.name
27	Antiretroviral Followup Study	\N	Antiretroviral Followup Study	\N	\N	Y	\N	LARC	2020-01-22 21:46:41.434205	\N	\N	project.ARVFollowupStudy.name
28	Viral Load Results	\N	Viral Load Results	\N	\N	Y	\N	LARC	2020-01-22 21:46:41.434205	\N	\N	project.VLStudy.name
\.


--
-- Data for Name: project_organization; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.project_organization (project_id, org_id) FROM stdin;
\.


--
-- Data for Name: project_parameter; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.project_parameter (id, projparam_type, operation, value, project_id, param_name) FROM stdin;
\.


--
-- Name: project_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.project_seq', 5, false);


--
-- Data for Name: provider; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.provider (id, npi, person_id, external_id, provider_type, lastupdated) FROM stdin;
\.


--
-- Name: provider_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.provider_seq', 1, false);


--
-- Data for Name: qa_event; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qa_event (id, name, description, is_billable, reporting_sequence, reporting_text, test_id, is_holdable, lastupdated, type, category, display_key) FROM stdin;
21	No Sample	No sample with order	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.noSample
22	No form	No form with sample	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.noForm
23	Incorrect Form		\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.formNotCorrect
24	Analysis Issue	There was a problem with the analysis	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.analysis.issue
25	Coagulated	Coagulated sample	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.colagulated
26	Hemolytic	Hemolytic sample	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.hemolytic
27	Insufficient	Insufficient Sample	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.insufficient
28	Inadequate	Inadequate Sample	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.inadequate
29	Mislabeled	Tube has been mislabeled	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.mislabled
30	Broken	Tube is cracked/broken	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.broken
31	Transmission Delay 	There was a dely in transmission	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.delay
32	Temperature	Wrong temperature	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.temperature
33	Other	Other	\N	\N	\N	\N	Y	2012-10-24 11:58:25.819012	\N	\N	qa_event.other
41	Sample_LA	Echantillon lactescent	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Sample_LA
42	Sample_OP	Echantillon opalescent	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Sample_OP
43	DBS_DI	DBS:Elution du disque DBS impossible	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_DI
44	No_ID_Prev	Absence de l’identité du préleveur	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.No_ID_Prev
45	No_HR_Prev	Absence de l’heure du prélèvement	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.No_HR_Prev
46	Error_Sample	Erreur de tube de prélèvement	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Error_Sample
47	Sample_VL_Late	Echantillon pour charge virale de plus de 6h	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Sample_VL_Late
48	Error_Prev_Demo	Discordance d’information entre Fiche de prélèvement et Fiche démographique	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Error_Prev_Demo
49	adult	DBS:Age de l’enfant > 18 mois	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.adult
50	Date_1	DBS:Date de prélèvement au-delà d’un (1) mois	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Date_1
51	DBS_3	DBS: Nombre de spot rempli  < 3	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_3
52	Diametre	DBS:Diamètre des spots < 5mm	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Diametre
53	DBS_C	DBS spot de sang coagulé	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_C
54	DBS_I	DBS spot de sang insuffisant	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_I
55	DBS_D	DBS spot de sang dilué par l’alcool	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_D
56	DBS_E	DBS:Carte DBS sans enveloppe glassine (si plusieurs DBS dans un sachet Ziplock)	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_E
57	DBS_NC	DBS:Carte DBS non conforme (différente du Whatman 903)	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_NC
58	DBS_NI	DBS non identifié	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_NI
59	DBS_MI	DBS mal identifié (Identité discordante sur DBS et fiche)	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_MI
60	DBS_SF	DBS sans fiche	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.DBS_SF
61	Fiche_no_DBS	DBS:Fiche de prélèvement sans échantillon DBS	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Fiche_no_DBS
62	Hour_No_VL	Heure de prélèvement non notifiée pour la charge virale	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Hour_No_VL
63	Prel_NN	Nom du préleveur non notifié	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Prel_NN
64	Sample_MI	Echantillon non ou mal identifié 	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Sample_MI
65	No_EDTA	VL:Demande de charge virale sans second tube EDTA 	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.No_EDTA
66	Sample_NF	Echantillon sur tube reçu sans glacière ou dans une glacière non réfrigérée	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Sample_NF
67	Sample_Code	Erreur de codification de l’échantillon 	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Sample_Code
68	Tube_I	Tube de prélèvement inapproprié 	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Tube_I
69	EDTA_Volume	 VL:Volume de sang total sur Tube EDTA pour demande de charge virale insuffisant (moins de la moitié du tube)	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.EDTA_Volume
70	PB_Identity	Discordance d’identité fiche entre de prélèvement et fiche démographique	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.PB_Identity
71	Order_NF	VL:Demande de charge virale sans fiche de demande appropriée 	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Order_NF
72	Order_Late	Retard de transmission des échantillons en tube 	\N	\N	\N	\N	Y	2020-01-22 21:46:42.005875	\N	\N	qa_event.Order_Late
\.


--
-- Name: qa_event_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.qa_event_seq', 80, true);


--
-- Data for Name: qa_observation; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qa_observation (id, observed_id, observed_type, qa_observation_type_id, value_type, value, lastupdated) FROM stdin;
\.


--
-- Name: qa_observation_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.qa_observation_seq', 1, false);


--
-- Data for Name: qa_observation_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qa_observation_type (id, name, description, lastupdated) FROM stdin;
1	authorizer	The name of the person who authorized the event	2012-10-24 11:58:24.454153-07
2	section	The section in which this happened	2012-10-24 11:58:24.454153-07
3	documentNumber	The qa document tracking number	2012-10-24 11:58:24.454153-07
\.


--
-- Name: qa_observation_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.qa_observation_type_seq', 3, true);


--
-- Data for Name: qc; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qc (id, uom_id, sys_user_id, name, source, lot_number, prepared_date, prepared_volume, usable_date, expire_date) FROM stdin;
\.


--
-- Data for Name: qc_analytes; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qc_analytes (id, qcanaly_type, value, analyte_id) FROM stdin;
\.


--
-- Data for Name: qrtz_blob_triggers; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_blob_triggers (trigger_name, trigger_group, blob_data) FROM stdin;
\.


--
-- Data for Name: qrtz_calendars; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_calendars (calendar_name, calendar) FROM stdin;
\.


--
-- Data for Name: qrtz_cron_triggers; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_cron_triggers (trigger_name, trigger_group, cron_expression, time_zone_id) FROM stdin;
\.


--
-- Data for Name: qrtz_fired_triggers; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_fired_triggers (entry_id, trigger_name, trigger_group, is_volatile, instance_name, fired_time, priority, state, job_name, job_group, is_stateful, requests_recovery) FROM stdin;
\.


--
-- Data for Name: qrtz_job_details; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_job_details (job_name, job_group, description, job_class_name, is_durable, is_volatile, is_stateful, requests_recovery, job_data) FROM stdin;
\.


--
-- Data for Name: qrtz_job_listeners; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_job_listeners (job_name, job_group, job_listener) FROM stdin;
\.


--
-- Data for Name: qrtz_locks; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_locks (lock_name) FROM stdin;
TRIGGER_ACCESS
JOB_ACCESS
CALENDAR_ACCESS
STATE_ACCESS
MISFIRE_ACCESS
\.


--
-- Data for Name: qrtz_paused_trigger_grps; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_paused_trigger_grps (trigger_group) FROM stdin;
\.


--
-- Data for Name: qrtz_scheduler_state; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_scheduler_state (instance_name, last_checkin_time, checkin_interval) FROM stdin;
\.


--
-- Data for Name: qrtz_simple_triggers; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_simple_triggers (trigger_name, trigger_group, repeat_count, repeat_interval, times_triggered) FROM stdin;
\.


--
-- Data for Name: qrtz_trigger_listeners; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_trigger_listeners (trigger_name, trigger_group, trigger_listener) FROM stdin;
\.


--
-- Data for Name: qrtz_triggers; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.qrtz_triggers (trigger_name, trigger_group, job_name, job_group, is_volatile, description, next_fire_time, prev_fire_time, priority, trigger_state, trigger_type, start_time, end_time, calendar_name, misfire_instr, job_data) FROM stdin;
\.


--
-- Data for Name: quartz_cron_scheduler; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.quartz_cron_scheduler (id, cron_statement, last_run, active, run_if_past, name, job_name, display_key, description_key) FROM stdin;
1	never	\N	f	t	send site indicators	sendSiteIndicators	schedule.name.sendSiteIndicators	schedule.description.sendSiteIndicators
2	never	\N	f	t	gather site indicators	gatherSiteIndicators	schedule.name.gatherSiteIndicators	schedule.description.gatherSiteIndicators
3	never	\N	f	t	send malaria surviellance report	sendMalariaSurviellanceReport	schedule.name.sendMalariaServiellanceReport	schedule.description.sendMalariaServiellanceReport
\.


--
-- Name: quartz_cron_scheduler_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.quartz_cron_scheduler_seq', 3, true);


--
-- Data for Name: receiver_code_element; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.receiver_code_element (id, identifier, text, code_system, lastupdated, message_org_id, code_element_type_id) FROM stdin;
1	40981-3	FLUA RNA XXX Ql PCR	LN	2007-03-07 15:33:45.297	22	1
2	40982-1	FLUB RNA XXX Ql PCR	LN	2007-03-07 15:34:15.205	22	1
3	FLU A (H1)	Influenza A subtype H1	L	2007-03-07 15:34:45.306	22	1
4	FLU A (H3)	Influenza A subtype H3	L	2007-03-07 15:35:15.408	22	1
\.


--
-- Name: receiver_code_element_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.receiver_code_element_seq', 21, false);


--
-- Data for Name: reference_tables; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.reference_tables (id, name, keep_history, is_hl7_encoded, lastupdated) FROM stdin;
40	STATUS_OF_SAMPLE	Y	Y	\N
39	NOTE	Y	N	\N
1	SAMPLE	Y	N	\N
2	GENDER	Y	N	\N
3	SAMPLE_ORGANIZATION	Y	N	\N
4	ANALYSIS	Y	N	\N
5	TEST	Y	Y	\N
6	CITY	Y	N	\N
7	ANALYTE	Y	Y	\N
8	COUNTY	Y	N	\N
9	DICTIONARY	Y	N	\N
10	DICTIONARY_CATEGORY	Y	N	\N
11	LABEL	Y	N	\N
12	METHOD	Y	N	\N
13	PANEL	Y	N	\N
14	PANEL_ITEM	Y	N	\N
15	PATIENT	Y	N	\N
16	PERSON	Y	N	\N
17	PROGRAM	Y	N	\N
18	PROJECT	Y	N	\N
19	PROVIDER	Y	N	\N
20	REGION	Y	N	\N
21	RESULT	Y	N	\N
22	SAMPLE_DOMAIN	Y	N	\N
23	SAMPLE_ITEM	Y	N	\N
24	SAMPLE_PROJECTS	Y	N	\N
25	SCRIPTLET	Y	N	\N
26	SOURCE_OF_SAMPLE	Y	N	\N
27	STATE_CODE	Y	N	\N
28	SYSTEM_USER	Y	N	\N
29	TEST_SECTION	Y	N	\N
30	TEST_ANALYTE	Y	N	\N
31	TEST_REFLEX	Y	N	\N
32	TEST_RESULT	Y	N	\N
33	TEST_TRAILER	Y	N	\N
34	TYPE_OF_SAMPLE	Y	N	\N
35	TYPE_OF_TEST_RESULT	Y	N	\N
36	UNIT_OF_MEASURE	Y	Y	\N
37	ZIP_CODE	Y	N	\N
38	ORGANIZATION	Y	N	\N
45	SAMPLE_HUMAN	Y	N	\N
46	QA_EVENT	Y	N	\N
48	ANALYSIS_QAEVENT	Y	N	\N
47	ACTION	Y	N	\N
49	ANALYSIS_QAEVENT_ACTION	Y	N	\N
70	REFERENCE_TABLES	N	N	\N
41	CODE_ELEMENT_TYPE	Y	N	\N
42	CODE_ELEMENT_XREF	Y	N	\N
43	MESSAGE_ORG	Y	N	\N
44	RECEIVER_CODE_ELEMENT	Y	N	\N
50	LOGIN_USER	Y	N	\N
51	SYSTEM_MODULE	Y	N	\N
52	SYSTEM_USER_MODULE	Y	N	\N
53	SYSTEM_USER_SECTION	Y	N	\N
110	SAMPLE_NEWBORN	Y	N	\N
111	PATIENT_RELATIONS	Y	N	\N
112	PATIENT_IDENTITY	Y	N	\N
113	PATIENT_PATIENT_TYPE	Y	N	\N
130	PATIENT_TYPE	N	N	\N
154	RESULT_LIMITS	Y	N	2009-02-10 17:11:57.227
155	RESULT_SIGNATURE	Y	N	2009-02-20 13:05:56.666
167	INVENTORY_LOCATION	N	N	2009-03-19 12:20:50.594
168	INVENTORY_ITEM	N	N	2009-03-19 12:20:50.594
169	INVENTORY_RECEIPT	N	N	2009-03-19 12:20:50.594
171	RESULT_INVENTORY	Y	N	2009-03-25 16:20:17.301
172	SYSTEM_ROLE	Y	N	2009-05-20 09:56:52.877513
173	SYSTEM_ROLE_MODULE	Y	N	2009-06-05 11:49:44.562736
174	SYSTEM_ROLE	Y	N	2009-06-05 11:50:56.86615
175	SYSTEM_USER_ROLE	Y	N	2009-06-05 11:59:25.708258
176	SYSTEM_USER_ROLE	Y	N	2009-06-05 12:03:40.526192
177	SYSTEM_ROLE	Y	N	2009-06-05 12:04:41.627999
178	SYSTEM_USER_ROLE	Y	N	2009-06-05 12:04:48.416696
179	SYSTEM_ROLE_MODULE	Y	N	2009-06-05 12:05:01.033811
182	analyzer	Y	N	2009-11-25 15:35:31.308859
183	analyzer_results	Y	N	2009-11-25 15:35:31.569744
184	site_information	Y	N	2010-03-23 17:04:19.671634
187	observation_history_type	Y	N	2010-04-28 14:13:23.717515
186	observation_history	Y	N	2010-04-21 10:38:59.516839
185	observation_history_type	Y	N	2010-04-21 10:38:50.05707
188	SAMPLE_QAEVENT	Y	N	2010-10-28 06:12:39.992393
189	referral_reason	Y	N	2010-10-28 06:13:55.299708
190	referral_type	Y	N	2010-10-28 06:13:55.299708
191	referral	Y	N	2010-10-28 06:13:55.299708
192	referral_result	Y	N	2010-11-23 10:30:22.045552
193	org_hl7_encoding_type	Y	N	2011-03-29 15:37:16.309479
197	address_part	Y	N	2011-03-29 15:37:16.455514
198	person_address	Y	N	2011-03-29 15:37:16.455514
199	organization_address	Y	N	2011-03-29 15:37:16.455514
200	organization_contact	Y	N	2011-03-29 15:37:16.469568
201	MENU	Y	N	2011-06-23 15:45:30.149806
202	SITE_INFORMATION_DOMAIN	Y	N	2011-06-27 12:43:01.432582
203	QUARTZ_CRON_SCHEDULER	Y	N	2011-06-28 16:58:12.97017
204	REPORT_QUEUE_TYPE	Y	N	2011-06-28 16:58:16.214669
205	REPORT_QUEUE	Y	N	2011-06-28 16:58:16.241375
206	REPORT_EXTERNAL_IMPORT	Y	N	2011-07-19 12:03:32.332874
207	document_type	Y	N	2011-09-26 11:52:19.215432
208	document_track	Y	N	2011-09-26 11:52:19.215432
209	ANALYZER_TEST_MAP	Y	N	2011-12-27 15:41:43.751219
210	PATIENT_IDENTITY_TYPE	Y	N	2011-12-27 15:41:43.751219
211	SAMPLETYPE_TEST	Y	N	2011-12-27 15:41:43.751219
212	SAMPLETYPE_PANEL	Y	N	2011-12-27 15:41:43.751219
213	SAMPLE_REQUESTER	Y	N	2011-12-27 15:41:43.751219
195	TEST_CODE	Y	N	2011-03-29 15:37:16.309479
194	TEST_CODE_TYPE	Y	N	2011-03-29 15:37:16.309479
214	QA_OBSERVATION	Y	N	2012-10-24 11:58:24.383199
215	ELECTROINIC_ORDER	Y	N	2013-04-17 10:44:01.376336
216	LOCALIZATION	Y	N	\N
217	REFERRING_TEST_RESULT	Y	N	2020-01-22 21:46:35.774031
218	BARCODE_LABEL_INFO	Y	N	\N
219	HL7_MESSAGE_OUT	Y	N	\N
220	TYPE_OF_DATA_INDICATOR	Y	N	\N
221	DATA_INDICATOR	Y	N	\N
222	DATA_RESOURCE	Y	N	\N
223	DATA_VALUE	Y	N	\N
224	nc_event	Y	N	\N
225	nce_action_log	Y	N	\N
226	nce_category	Y	N	\N
227	nce_specimen	Y	N	\N
228	nce_type	Y	N	\N
229	report	Y	N	\N
\.


--
-- Name: reference_tables_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.reference_tables_seq', 229, true);


--
-- Data for Name: referral; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.referral (id, analysis_id, organization_id, organization_name, send_ready_date, sent_date, result_recieved_date, referral_reason_id, referral_type_id, requester_name, lastupdated, canceled, referral_request_date) FROM stdin;
\.


--
-- Data for Name: referral_reason; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.referral_reason (id, name, description, display_key, lastupdated) FROM stdin;
1	Test not performed	Test was not performed	referral.reason.testNotPerformed	2011-02-16 12:45:59.445136-08
2	Confirmation requested	Confirmation requested	referral.reason.confirmation	2011-02-16 12:45:59.445136-08
3	Further testing required 	Further testing required 	referral.reason.moreTests	2011-02-16 12:45:59.445136-08
4	Reagent expired	Reagent expired	referral.reason.reagent.expired	2011-02-16 12:45:59.445136-08
5	Reagents unavailable 	Reagents unavailable 	referral.reason.reagent.unavailabe	2011-02-16 12:45:59.445136-08
6	Equipment failure	Equipment failure	referral.reason.equipment	2011-02-16 12:45:59.445136-08
7	Verification of EQA	Verification of EQA	referral.reason.EQA.verification	2011-02-16 12:45:59.445136-08
8	Specimen sent for serotyping	Specimen sent for serotyping	referral.reason.serotyping	2011-02-16 12:45:59.445136-08
9	EQA by Repeat Testing	EQA by Repeat Testing	referral.reason.EQA.testing	2011-02-16 12:45:59.445136-08
10	Other	Some reason not on list	referral.reason.other	2011-02-16 12:45:59.464429-08
\.


--
-- Name: referral_reason_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.referral_reason_seq', 10, true);


--
-- Data for Name: referral_result; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.referral_result (id, referral_id, test_id, result_id, referral_report_date, lastupdated) FROM stdin;
\.


--
-- Name: referral_result_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.referral_result_seq', 44, true);


--
-- Name: referral_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.referral_seq', 172, true);


--
-- Data for Name: referral_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.referral_type (id, name, description, display_key, lastupdated) FROM stdin;
1	Confirmation	Sent out to confirm result	referral.type.confirmation	2011-02-16 12:45:59.428687-08
2	QA	Quality assurance	referral.type.qa	2011-02-16 12:45:59.428687-08
3	EQA	External Qaulity Assurance	referral.type.eqa	2011-02-16 12:45:59.428687-08
\.


--
-- Name: referral_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.referral_type_seq', 3, true);


--
-- Data for Name: referring_test_result; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.referring_test_result (id, sample_item_id, test_name, result_value, lastupdated) FROM stdin;
\.


--
-- Name: referring_test_result_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.referring_test_result_seq', 1, false);


--
-- Data for Name: region; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.region (id, region, lastupdated) FROM stdin;
1	Central	2006-10-24 08:46:59.692
2	Metro	2006-11-08 10:47:34.147
3	Northeast	2006-10-24 08:47:01.645
4	Northwest	2006-10-24 08:47:02.427
5	South Central	2006-10-24 08:47:03.427
6	Southeast	2006-10-24 08:47:04.489
7	Southwest	2006-10-24 08:47:06.317
8	West Central	2006-10-24 08:47:09.942
9	update	2006-10-24 08:47:09.442
\.


--
-- Name: region_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.region_seq', 1, false);


--
-- Data for Name: report; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.report (id, category, sort_order, menu_element_id, display_key, name, is_visible) FROM stdin;
1	3	0	menu_reports_status_patient.classique	openreports.patientTestStatus.classique	Patient Status report classic version	t
2	3	1	menu_reports_status_patient.vreduit	openreports.patientTestStatus.vreduit	Patient Status report reduced version	t
3	4	1	menu_reports_aggregate_all	openreports.all.tests.aggregate	Summary of all tests	t
4	4	2	menu_reports_aggregate_hiv	openreports.hiv.aggregate	HIV Test Summary	t
5	6	1	menu_activity_report_test	banner.menu.workplan.test	By Test Type	t
6	6	2	menu_activity_report_panel	banner.menu.workplan.panel	By Panel Type	t
7	6	3	menu_activity_report_bench	banner.menu.workplan.bench	By Unit	t
8	5	4	menu_reports_referred	reports.label.referred.out	Referred Tests Reports	t
9	7	1	menu_reports_nonconformity_date	reports.nonConformity.byDate.report	By Date	t
10	7	2	menu_reports_nonconformity_section	reports.nonConformity.bySectionReason.report	By Unit and Reason	t
11	5	6	menu_reports_validation_backlog	banner.menu.report.validation.backlog	Delayed Validation	t
12	5	7	menu_reports_auditTrail	reports.auditTrail	Audit Trail	t
13	1	3	menu_reports_export_routine	reports.export.byDate_routine	Export Routice CSV File	t
14	1	9	menu_reports_arv_initial1	reports.patient.ARVInitial.version1	ARV Initial Version 1	t
15	2	9	menu_reports_arv_initial2	reports.patient.ARVInitial.version2	ARV Initial Version 2	t
16	3	9	menu_reports_arv_followup1	reports.patient.ARVFollowup.version1	ARV Follow-up Version 1	t
17	4	9	menu_reports_arv_followup2	reports.patient.ARVFollowup.version2	ARV Follow-up Version 2	t
18	5	9	menu_reports_arv_all	reports.patient.ARV.version1	ARV-Version 1	t
19	1	10	menu_reports_eid_version1	openreports.patient.EID.version1	EID Version 1	t
20	2	10	menu_reports_eid_version2	openreports.patient.EID.version2	EID Version 2	t
21	1	11	menu_reports_vl_version1	openreports.patient.VL.version1	VL Version Nationale	t
22	1	12	menu_reports_indeterminate_version1	reports.patient.Indeterminate.version1	Indeterminate Version 1	t
23	2	12	menu_reports_indeterminate_version2	reports.patient.Indeterminate.version2	Indeterminate Version 2	t
24	3	12	menu_reports_indeterminate_location	reports.patient.Indeterminate.location	Indeterminate by Service	t
25	4	8	menu_reports_special	project.SpecialRequestStudy.name	Special Request	t
26	5	8	menu_reports_patient_collection	project.SpecialRequestStudy.name	Collected ARV Patient Report	t
27	6	8	menu_reports_patient_associated	patient.report.associated.name	Associated Patient Report	t
28	1	13	menu_reports_indicator_performance	reports.label.indicator.performance	Section Performance	t
29	2	13	menu_reports_validation_backlog.study	banner.menu.report.validation.backlog	Delayed Validation	t
30	1	14	menu_reports_nonconformity_date.study	reports.nonConformity.byDate.report	By Date	t
31	2	14	menu_reports_nonconformity_section.study	reports.nonConformity.bySectionReason.report	By Unit and Reason	t
32	3	14	menu_reports_nonconformity.Labno	reports.nonConformity.byLabno.report	By Labno	t
33	4	14	menu_reports_nonconformity_notification.study	reports.nonConformity.notification.report	Non-conformity notification	t
34	5	14	menu_reports_followupRequired_ByLocation.study	reports.followupRequired.byLocation	Follow-up required	t
35	1	15	menu_reports_export_general	reports.export.general	General Export	t
36	2	15	menu_reports_export_valid	reports.export.valid	Valid Export	t
37	3	15	menu_reports_export_specific	reports.export.specific	Viral Load Data Export	t
\.


--
-- Data for Name: report_external_export; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.report_external_export (id, event_date, collection_date, sent_date, type, data, lastupdated, send_flag, bookkeeping) FROM stdin;
\.


--
-- Data for Name: report_external_import; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.report_external_import (id, sending_site, event_date, recieved_date, type, updated_flag, data, lastupdated) FROM stdin;
\.


--
-- Name: report_external_import_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.report_external_import_seq', 1, false);


--
-- Name: report_id_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.report_id_seq', 37, true);


--
-- Name: report_queue_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.report_queue_seq', 1, false);


--
-- Data for Name: report_queue_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.report_queue_type (id, name, description) FROM stdin;
1	labIndicator	Lab indicator reports.  Number of tests run etc
2	Results	Result sharing with iSante
3	malariaCase	malaria case report
\.


--
-- Name: report_queue_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.report_queue_type_seq', 3, true);


--
-- Data for Name: requester_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.requester_type (id, requester_type) FROM stdin;
1	organization
2	provider
\.


--
-- Name: requester_type_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.requester_type_seq', 2, false);


--
-- Data for Name: result; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.result (id, analysis_id, sort_order, is_reportable, result_type, value, analyte_id, test_result_id, lastupdated, min_normal, max_normal, parent_id, significant_digits, "grouping") FROM stdin;
\.


--
-- Data for Name: result_inventory; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.result_inventory (id, inventory_location_id, result_id, description, lastupdated) FROM stdin;
\.


--
-- Name: result_inventory_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.result_inventory_seq', 1, false);


--
-- Data for Name: result_limits; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age, gender, low_normal, high_normal, low_valid, high_valid, lastupdated, normal_dictionary_id, always_validate) FROM stdin;
1	1	4	0	Infinity	 	7	40	7	350	2012-10-24 11:58:25.588497	\N	f
2	2	4	0	Infinity	 	3	40	3	350	2012-10-24 11:58:25.588497	\N	f
3	3	4	0	Infinity	 	0.699999999999999956	1.10000000000000009	0.100000000000000006	5	2012-10-24 11:58:25.588497	\N	f
4	4	4	0	Infinity	 	6	13	2.0299999999999998	150	2012-10-24 11:58:25.588497	\N	f
5	5	4	0	Infinity	 	1	486	1	1000	2012-10-24 11:58:25.588497	\N	f
6	6	4	0	Infinity	 	36	50	0	100	2012-10-24 11:58:25.588497	\N	f
7	7	4	0	Infinity	 	1.5	2.60000000000000009	0.0100000000000000002	5	2012-10-24 11:58:25.588497	\N	f
8	8	4	0	Infinity	 	0.349999999999999978	0.699999999999999956	0	150	2012-10-24 11:58:25.588497	\N	f
9	9	4	0	Infinity	 	0.349999999999999978	1.69999999999999996	0.100000000000000006	7	2012-10-24 11:58:25.588497	\N	f
10	10	4	0	Infinity	 	0.0500000000000000028	0.200000000000000011	0.0500000000000000028	1.5	2012-10-24 11:58:25.588497	\N	f
11	13	4	120	Infinity	 	4	10	0	100	2012-10-24 11:58:25.588497	\N	f
12	13	4	0	3	 	10	25	0	100	2012-10-24 11:58:25.588497	\N	f
13	13	4	3	8	 	8	15	0	100	2012-10-24 11:58:25.588497	\N	f
14	13	4	8	12	 	6	15	0	100	2012-10-24 11:58:25.588497	\N	f
15	13	4	12	48	 	4.5	13	0	100	2012-10-24 11:58:25.588497	\N	f
16	13	4	48	120	 	4	10	0	100	2012-10-24 11:58:25.588497	\N	f
17	14	4	12	Infinity	M	4.5	6	0	10	2012-10-24 11:58:25.588497	\N	f
18	14	4	12	Infinity	F	4.79999999999999982	5.5	0	10	2012-10-24 11:58:25.588497	\N	f
19	14	4	0	3	 	5	6.20000000000000018	0	10	2012-10-24 11:58:25.588497	\N	f
20	14	4	3	12	 	3.60000000000000009	5	0	10	2012-10-24 11:58:25.588497	\N	f
21	15	4	12	Infinity	F	12	16	0	30	2012-10-24 11:58:25.588497	\N	f
22	15	4	12	Infinity	M	13	18	0	30	2012-10-24 11:58:25.588497	\N	f
23	15	4	0	3	 	16	20	0	30	2012-10-24 11:58:25.588497	\N	f
24	15	4	3	12	 	12	16	0	30	2012-10-24 11:58:25.588497	\N	f
25	16	4	12	Infinity	F	37	47	0	80	2012-10-24 11:58:25.588497	\N	f
26	16	4	12	Infinity	M	40	52	0	80	2012-10-24 11:58:25.588497	\N	f
27	16	4	0	3	 	44	62	0	80	2012-10-24 11:58:25.588497	\N	f
28	16	4	3	12	 	36	42	0	80	2012-10-24 11:58:25.588497	\N	f
29	17	4	3	Infinity	 	85	95	0	266	2012-10-24 11:58:25.588497	\N	f
30	17	4	0	3	 	106	106	0	266	2012-10-24 11:58:25.588497	\N	f
31	18	4	0	Infinity	 	27	31	0	50	2012-10-24 11:58:25.588497	\N	f
32	19	4	0	Infinity	 	32	36	0	100	2012-10-24 11:58:25.588497	\N	f
33	20	4	0	Infinity	 	150	400	0	1500	2012-10-24 11:58:25.588497	\N	f
34	21	4	0	Infinity	 	45	70	0	100	2012-10-24 11:58:25.588497	\N	f
35	22	4	0	Infinity	 	1500	7000	0	100000	2012-10-24 11:58:25.588497	\N	f
36	23	4	0	Infinity	 	0	4	0	100	2012-10-24 11:58:25.588497	\N	f
37	24	4	0	Infinity	 	0	400	0	100000	2012-10-24 11:58:25.588497	\N	f
38	25	4	0	Infinity	 	0	0.5	0	100	2012-10-24 11:58:25.588497	\N	f
39	26	4	0	Infinity	 	0	50	0	100000	2012-10-24 11:58:25.588497	\N	f
40	27	4	0	Infinity	 	20	40	0	100	2012-10-24 11:58:25.588497	\N	f
41	28	4	0	Infinity	 	1500	4000	0	100000	2012-10-24 11:58:25.588497	\N	f
42	29	4	0	Infinity	 	2	10	0	100	2012-10-24 11:58:25.588497	\N	f
43	30	4	0	Infinity	 	0	1000	0	100000	2012-10-24 11:58:25.588497	\N	f
44	34	4	0	Infinity	 	400	1750	0	3000	2012-10-24 11:58:25.588497	\N	f
45	35	4	0	Infinity	 	35	55	0	5000	2012-10-24 11:58:25.588497	\N	f
46	37	4	0	Infinity	 	1	300	1	200000	2012-10-24 11:58:25.588497	\N	f
47	38	4	0	Infinity	 	1	300	1	200000	2012-10-24 11:58:25.588497	\N	f
49	31	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
50	32	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
51	33	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
52	36	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
53	39	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
54	40	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
55	41	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
56	42	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
57	43	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
58	44	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
59	45	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
60	46	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
61	47	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
62	48	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
63	49	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
64	50	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
65	51	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
66	52	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2012-10-24 11:58:25.630678	1103	f
67	53	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2013-10-10 16:52:59.923212	1103	f
68	54	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2013-10-10 16:52:59.923212	1103	f
69	55	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2013-10-10 16:52:59.923212	1103	f
70	56	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2013-10-10 16:52:59.923212	1103	f
71	57	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2013-10-10 16:52:59.923212	1103	f
72	58	2	0	Infinity	\N	-Infinity	Infinity	-Infinity	Infinity	2013-10-10 16:52:59.923212	1103	f
73	159	4	0	Infinity	 	0.699999999999999956	1.10000000000000009	0	5	2020-01-22 21:46:41.983534	\N	f
74	194	4	0	Infinity	M	5.29999999999999982	12.1999999999999993	0	100	2020-01-22 21:46:41.983534	\N	f
75	181	4	0	Infinity	M	0.299999999999999989	0.819999999999999951	0	10	2020-01-22 21:46:41.983534	\N	f
76	196	4	0	Infinity	M	21.8000000000000007	53.1000000000000014	0	100	2020-01-22 21:46:41.983534	\N	f
77	180	4	0	Infinity	M	1.32000000000000006	3.56999999999999984	0	10	2020-01-22 21:46:41.983534	\N	f
78	195	4	0	Infinity	M	0.200000000000000011	1.19999999999999996	0	10	2020-01-22 21:46:41.983534	\N	f
79	197	4	0	Infinity	M	0.800000000000000044	7	0	10	2020-01-22 21:46:41.983534	\N	f
80	182	4	0	Infinity	M	0.0400000000000000008	0.540000000000000036	0	10	2020-01-22 21:46:41.983534	\N	f
81	193	4	0	Infinity	M	34	67.9000000000000057	0	100	2020-01-22 21:46:41.983534	\N	f
82	179	4	0	Infinity	M	1.78000000000000003	5.37999999999999989	0	100	2020-01-22 21:46:41.983534	\N	f
83	178	4	0	Infinity	M	163	337	0	1500	2020-01-22 21:46:41.983534	\N	f
84	185	4	0	Infinity	M	32.2999999999999972	36.5	0	50	2020-01-22 21:46:41.983534	\N	f
85	189	4	0	Infinity	M	0.0500000000000000028	0.200000000000000011	0	100	2020-01-22 21:46:41.983534	\N	f
86	186	4	0	Infinity	M	4.62999999999999989	6.08000000000000007	0	10	2020-01-22 21:46:41.983534	\N	f
87	186	4	0	Infinity	F	3.93000000000000016	5.21999999999999975	0	10	2020-01-22 21:46:41.983534	\N	f
88	187	4	0	Infinity	F	11.1999999999999993	15.6999999999999993	0	30	2020-01-22 21:46:41.983534	\N	f
89	187	4	0	Infinity	M	13.6999999999999993	17.5	0	30	2020-01-22 21:46:41.983534	\N	f
90	184	4	0	Infinity	F	34.1000000000000014	44.8999999999999986	0	100	2020-01-22 21:46:41.983534	\N	f
91	184	4	0	Infinity	M	40.1000000000000014	51	0	100	2020-01-22 21:46:41.983534	\N	f
92	188	4	3	Infinity	F	79.4000000000000057	94.7999999999999972	0	266	2020-01-22 21:46:41.983534	\N	f
93	188	4	0	Infinity	M	79	92.2000000000000028	0	266	2020-01-22 21:46:41.983534	\N	f
94	189	4	0	Infinity	F	25.6000000000000014	32.2000000000000028	0	50	2020-01-22 21:46:41.983534	\N	f
95	185	4	0	Infinity	F	32.2000000000000028	35.5	0	100	2020-01-22 21:46:41.983534	\N	f
96	178	4	0	Infinity	F	182	369	0	1500	2020-01-22 21:46:41.983534	\N	f
97	179	4	0	Infinity	F	1.56000000000000005	6.12999999999999989	0	100	2020-01-22 21:46:41.983534	\N	f
98	193	4	0	Infinity	F	34	71.0999999999999943	0	100	2020-01-22 21:46:41.983534	\N	f
99	182	4	0	Infinity	F	0.0400000000000000008	0.359999999999999987	0	10	2020-01-22 21:46:41.983534	\N	f
100	197	4	0	Infinity	F	0.699999999999999956	5.79999999999999982	0	10	2020-01-22 21:46:41.983534	\N	f
101	183	4	0	Infinity	 	0.0100000000000000002	0.0800000000000000017	0	10	2020-01-22 21:46:41.983534	\N	f
102	195	4	0	Infinity	F	0.100000000000000006	1.19999999999999996	0	10	2020-01-22 21:46:41.983534	\N	f
103	180	4	0	Infinity	F	1.17999999999999994	3.74000000000000021	0	10	2020-01-22 21:46:41.983534	\N	f
104	196	4	0	Infinity	F	19.3000000000000007	51.7000000000000028	0	100	2020-01-22 21:46:41.983534	\N	f
105	181	4	0	Infinity	F	0.0200000000000000004	0.359999999999999987	0	10	2020-01-22 21:46:41.983534	\N	f
106	194	4	0	Infinity	F	4.70000000000000018	12.5	0	100	2020-01-22 21:46:41.983534	\N	f
107	160	4	0	Infinity	 	6	13	2.0299999999999998	100	2020-01-22 21:46:41.983534	\N	f
108	161	4	0	Infinity	 	7	40	7	300	2020-01-22 21:46:41.983534	\N	f
109	191	4	0	Infinity	 	35	55	0	5000	2020-01-22 21:46:41.983534	\N	f
110	192	4	0	Infinity	 	400	750	0	3000	2020-01-22 21:46:41.983534	\N	f
111	176	4	0	Infinity	 	3	40	3	3000	2020-01-22 21:46:41.983534	\N	f
112	177	4	0	Infinity	 	4	10	0	100	2020-01-22 21:46:41.983534	\N	f
\.


--
-- Name: result_limits_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.result_limits_seq', 112, true);


--
-- Name: result_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.result_seq', 1, false);


--
-- Data for Name: result_signature; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.result_signature (id, result_id, system_user_id, is_supervisor, lastupdated, non_user_name) FROM stdin;
\.


--
-- Name: result_signature_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.result_signature_seq', 1, false);


--
-- Data for Name: sample; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample (id, accession_number, package_id, domain, next_item_sequence, revision, entered_date, received_date, collection_date, client_reference, status, released_date, sticker_rcvd_flag, sys_user_id, barcode, transmission_date, lastupdated, spec_or_isolate, priority, status_id, referring_id, clinical_order_id, is_confirmation) FROM stdin;
\.


--
-- Data for Name: sample_domain; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_domain (id, domain_description, domain, lastupdated) FROM stdin;
28	ANIMAL SAMPLES	A	2006-11-08 10:58:03.229
29	ENVIRONMENTAL	E	2006-09-21 10:06:53
27	HUMAN SAMPLES	H	2006-09-21 10:06:01
2	NEWBORN SAMPLES	N	2008-10-31 15:19:03.544
\.


--
-- Name: sample_domain_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_domain_seq', 2, true);


--
-- Data for Name: sample_environmental; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_environmental (id, samp_id, is_hazardous, lot_nbr, description, chem_samp_num, street_address, multiple_unit, city, state, zip_code, country, collector, sampling_location) FROM stdin;
\.


--
-- Data for Name: sample_human; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_human (id, provider_id, samp_id, patient_id, lastupdated) FROM stdin;
\.


--
-- Name: sample_human_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_human_seq', 1, false);


--
-- Data for Name: sample_item; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_item (id, sort_order, sampitem_id, samp_id, source_id, typeosamp_id, uom_id, source_other, quantity, lastupdated, external_id, collection_date, status_id, collector) FROM stdin;
\.


--
-- Name: sample_item_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_item_seq', 1, false);


--
-- Data for Name: sample_newborn; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_newborn (id, weight, multi_birth, birth_order, gestational_week, date_first_feeding, breast, tpn, formula, milk, soy, jaundice, antibiotics, transfused, date_transfusion, medical_record_numeric, nicu, birth_defect, pregnancy_complication, deceased_sibling, cause_of_death, family_history, other, y_numeric, yellow_card, lastupdated) FROM stdin;
\.


--
-- Name: sample_org_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_org_seq', 112, false);


--
-- Data for Name: sample_organization; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_organization (id, org_id, samp_id, samp_org_type, lastupdated) FROM stdin;
\.


--
-- Data for Name: sample_pdf; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_pdf (id, accession_number, allow_view, barcode) FROM stdin;
\.


--
-- Name: sample_pdf_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_pdf_seq', 1, false);


--
-- Name: sample_proj_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_proj_seq', 1, false);


--
-- Data for Name: sample_projects; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_projects (samp_id, proj_id, is_permanent, id, lastupdated) FROM stdin;
\.


--
-- Data for Name: sample_qaevent; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_qaevent (id, qa_event_id, sample_id, completed_date, lastupdated, sampleitem_id, entered_date) FROM stdin;
\.


--
-- Data for Name: sample_qaevent_action; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_qaevent_action (id, sample_qaevent_id, action_id, created_date, lastupdated, sys_user_id) FROM stdin;
\.


--
-- Name: sample_qaevent_action_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_qaevent_action_seq', 1, false);


--
-- Name: sample_qaevent_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_qaevent_seq', 1, false);


--
-- Data for Name: sample_requester; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sample_requester (sample_id, requester_id, requester_type_id, lastupdated, id) FROM stdin;
\.


--
-- Name: sample_requester_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_requester_seq', 44, true);


--
-- Name: sample_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_seq', 1, false);


--
-- Name: sample_type_panel_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_type_panel_seq', 7, true);


--
-- Name: sample_type_test_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.sample_type_test_seq', 58, true);


--
-- Data for Name: sampletype_panel; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sampletype_panel (id, sample_type_id, panel_id) FROM stdin;
1	2	1
2	3	1
3	1	1
4	4	2
5	4	3
6	2	4
7	3	4
\.


--
-- Data for Name: sampletype_test; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.sampletype_test (id, sample_type_id, test_id, is_panel) FROM stdin;
1	2	1	f
2	2	2	f
3	3	3	f
4	2	4	f
5	2	5	f
6	1	6	f
7	2	7	f
8	2	8	f
9	2	9	f
10	1	10	f
11	1	11	f
12	1	12	f
13	4	13	f
14	4	14	f
15	4	15	f
16	4	16	f
17	4	17	f
18	4	18	f
19	4	19	f
20	4	20	f
21	4	21	f
22	4	22	f
23	4	23	f
24	4	24	f
25	4	25	f
26	4	26	f
27	4	27	f
28	4	28	f
29	4	29	f
30	4	30	f
31	2	31	f
32	3	32	f
33	4	33	f
34	4	34	f
35	4	35	f
36	2	36	f
37	4	37	f
38	4	38	f
39	2	39	f
40	3	40	f
41	3	41	f
42	2	42	f
43	4	43	f
44	3	44	f
45	2	45	f
46	4	46	f
47	3	47	f
48	2	48	f
49	3	49	f
50	2	50	f
51	3	51	f
52	2	52	f
53	3	53	f
54	2	54	f
55	4	55	f
56	3	56	f
57	2	57	f
58	4	58	f
\.


--
-- Data for Name: scriptlet; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.scriptlet (id, name, code_type, code_source, lastupdated) FROM stdin;
13	Ais Test	B	C	2006-12-13 11:00:01.748
11	Diane Test	T	Diane test	2006-11-01 13:34:49.667
12	SCRIPTLET	S	test	2006-11-08 10:58:32.964
1	HIV Status Indeterminate	I	HIV Indeterminate	2011-02-02 11:55:53.344606
2	HIV Status Negative	I	HIV N	2011-02-02 11:55:53.344606
3	HIV Status Positive	I	HIV Positive	2011-02-02 11:55:53.344606
\.


--
-- Name: scriptlet_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.scriptlet_seq', 3, true);


--
-- Data for Name: site_information; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.site_information (id, name, lastupdated, description, value, encrypted, domain_id, value_type, instruction_key, "group", schedule_id, tag, dictionary_category_id, description_key, name_key) FROM stdin;
100	firstNameCharset	\N	allowed characters in first name fields	.'a-zàâçéèêëîïôûùüÿñæœ -	f	18	text	instructions.validationconfig.firstname	0	\N	\N	\N	siteInfo.validationConfig.firstname	\N
101	lastNameCharset	\N	allowed characters in last name fields	.'a-zàâçéèêëîïôûùüÿñæœ -	f	18	text	instructions.validationconfig.lastname	0	\N	\N	\N	siteInfo.validationConfig.lastname	\N
102	userNameCharset	\N	allowed characters in username fields	a-zàâçéèêëîïôûùüÿñæœ ._@-	f	18	text	instructions.validationconfig.username	0	\N	\N	\N	siteInfo.validationConfig.username	\N
103	patientIdCharset	\N	allowed characters in patient identifier fields	a-z0-9/àâçéèêëîïôûùüÿñæœ	f	18	text	instructions.validationconfig.patientid	0	\N	\N	\N	siteInfo.validationConfig.patientid	\N
104	reportsDirectory	2020-01-22 21:46:59.769137-08	the directory for the reports	/reports	f	11	text	\N	0	\N	\N	\N	siteInfo.reportConfig.reportDirectory	\N
20	reflex_rules	2011-07-28 13:20:19.341221-07	What set of reflex rules are used. From a predefined list	\N	f	5	text	\N	0	\N	\N	\N	siteInfo.reflex_rules	\N
21	testUsageAggregationUrl	2011-07-28 13:20:19.341221-07	The url of the site to which test usage will be sent	\N	f	6	text	\N	0	\N	\N	\N	siteInfo.testUsageAggregationUrl	\N
22	testUsageAggregationUserName	2011-07-28 13:20:19.341221-07	The user name for accesses to the service for aggregating test usage	\N	f	6	text	\N	0	\N	\N	\N	siteInfo.testUsageAggregationUserName	\N
23	testUsageAggregationPassword	2011-07-28 13:20:19.341221-07	The password for accesser to the service for aggregating test usage	\N	f	6	text	\N	0	\N	\N	\N	siteInfo.testUsageAggregationPassword	\N
24	testUsageSendStatus	2011-07-29 15:20:48.289962-07	The status of what happened the last time an attempt was made to send the report	N/A	f	6	text	\N	0	\N	\N	\N	siteInfo.testUsageSendStatus	\N
9	useExternalPatientSource	2010-10-28 04:13:56.491221-07	Use an external source patient demographics true/false	false	f	1	boolean	\N	0	\N	\N	\N	siteInfo.useExternalPatientSource	\N
27	modify results role	2011-10-27 10:04:30.983894-07	Should a separate role be required to be able to modify results	false	f	9	boolean	\N	0	\N	\N	\N	siteInfo.modify.results.role	\N
28	modify results note required	2011-10-27 10:04:30.983894-07	Is a note required when results are modified	false	f	9	boolean	\N	0	\N	\N	\N	siteInfo.modify.results.note.required	\N
29	ResultTechnicianName	2011-11-04 17:08:50.863368-07	If true then the technician name is required for entering results	false	f	9	boolean	instructions.results.technician	0	\N	\N	\N	siteInfo.ResultTechnicianName	\N
30	autoFillTechNameBox	2011-11-04 17:08:50.863368-07	If the techs name is required on results then add a box for autofill	false	f	9	boolean	instructions.results.autofilltechbox	0	\N	\N	\N	siteInfo.autoFillTechNameBox	\N
31	autoFillTechNameUser	2011-11-04 17:08:50.863368-07	If the techs name is required on results then autofill with logged in user	false	f	9	boolean	instructions.results.autofilltech.user	0	\N	\N	\N	siteInfo.autoFillTechNameUser	\N
12	allowLanguageChange	2011-05-20 10:01:40.934926-07	Allows the user to change the language at login	true	f	1	boolean	\N	0	\N	\N	\N	siteInfo.allowLanguageChange	\N
36	trackPayment	2012-03-13 15:32:16.688023-07	If true track patient payment for services	true	f	10	boolean	instructions.patient.payment	0	\N	\N	\N	siteInfo.trackPayment	\N
38	statusRules	2012-03-16 14:09:50.468456-07	statusRules determine specific status values for the application, ex: LNSP_haiti	LNSP_haiti	f	11	text	\N	0	\N	\N	\N	siteInfo.statusRules	\N
39	reflexAction	2012-03-16 14:09:50.468456-07	reflexActions determine the meaning of the flags in reflexes, ex: RetroCI	LNSP_Haiti	f	11	text	\N	0	\N	\N	\N	siteInfo.reflexAction	\N
43	alertWhenInvalidResult	2012-04-03 09:41:04.884938-07	Should there be an alert when the user enters a result outside of the valid range	false	f	9	boolean	instructions.results.invalidAlert	0	\N	\N	\N	siteInfo.alertWhenInvalidResult	\N
25	resultReporting	2011-08-12 17:26:20.61669-07	Should reporting results electronically be enabled	false	f	8	boolean	\N	1	\N	enable	\N	siteInfo.resultReporting	\N
26	resultReportingURL	2011-08-12 17:26:20.61669-07	Where reporting results electronically should be sent	disable	f	8	text	\N	1	\N	url	\N	siteInfo.resultReportingURL	\N
44	malariaSurURL	2012-04-20 11:20:34.0183-07	The URL for malaria Surveillance reports		f	8	text	instructions.result.malaria.sur.url	2	\N	url	\N	siteInfo.malariaSurURL	\N
45	malariaSurReport	2012-04-20 11:20:34.0183-07	True to send reports, false otherwise	false	f	8	boolean	instructions.result.malaria.surveillance	2	3	enable	\N	siteInfo.malariaSurReport	\N
46	malariaCaseURL	2012-04-20 11:20:34.0183-07	The URL for malaria case reports		f	8	text	instructions.result.malaria.case.url	3	\N	url	\N	siteInfo.malariaCaseURL	\N
47	malariaCaseReport	2012-04-20 11:20:34.0183-07	True to send reports, false otherwise	false	f	8	boolean	instructions.result.malaria.case	3	\N	enable	\N	siteInfo.malariaCaseReport	\N
48	testUsageReporting	2012-05-02 13:18:22.637167-07	Should reporting testUsage electronically be enabled	false	f	6	boolean	instructions.test.usage	0	\N	enable	\N	siteInfo.testUsageReporting	\N
49	default language locale	2012-05-14 11:24:08.802607-07	The default language local	fr-FR	f	1	dictionary	\N	0	\N	\N	197	siteInfo.default.language.locale	\N
50	default date locale	2012-05-14 11:24:08.802607-07	The default date local	fr-FR	f	1	dictionary	\N	0	\N	\N	197	siteInfo.default.date.locale	\N
52	condenseNSF	2012-06-01 15:10:58.452758-07	Should NFS be represented as NFS or as individual tests	false	f	11	boolean	\N	0	\N	\N	\N	siteInfo.condenseNSF	\N
53	roleForPatientOnResults	2012-06-19 10:05:53.340299-07	Is patient information restricted to those in correct role	false	f	9	boolean	\N	0	\N	\N	\N	siteInfo.roleForPatientOnResults	\N
7	patientSearchLogOnUser	2010-10-28 04:13:56.491221-07	The user name for using the service		f	1	text	\N	0	\N	\N	\N	siteInfo.patientSearchLogOnUser	\N
8	patientSearchPassword	2010-10-28 04:13:56.491221-07	The password for using the service		f	1	text	\N	0	\N	\N	\N	siteInfo.patientSearchPassword	\N
6	patientSearchURL	2010-10-28 04:13:56.491221-07	The service URL from which to import patient demographics		f	1	text	\N	0	\N	\N	\N	siteInfo.patientSearchURL	\N
54	reportPageNumbers	2012-08-29 16:02:18.9191-07	use page numbers on reports	false	f	12	boolean	siteInformation.instruction.pageNumbers	0	\N	\N	\N	siteInfo.reportPageNumbers	\N
33	SiteName	2012-01-27 09:40:08.80713-08	The name of the site for reports and header		f	12	text	instructions.site.name	0	\N	\N	\N	siteInfo.SiteName	\N
34	lab director	2012-01-27 09:40:12.362877-08	The lab directors name for the reports		f	12	text	instructions.site.lab.director	0	\N	\N	\N	siteInfo.lab.director	\N
59	subject on workplan	2013-03-05 16:27:56.900559-08	Use the subject number on the workplan	false	f	13	boolean	instructions.workplan.subject	0	\N	\N	\N	siteInfo.subject.on.workplan	\N
60	next visit on workplan	2013-03-05 16:27:56.951592-08	Show the date of the next visit on the workplan	false	f	13	boolean	instructions.workplan.next.visit	0	\N	\N	\N	siteInfo.next.visit.on.workplan	\N
56	validate all results	2012-11-05 16:48:41.7299-08	all results should be validated even if normal	true	f	9	boolean	siteInformation.instruction.validate.all	0	\N	\N	\N	siteInfo.validate.all.results	\N
57	additional site info	2012-12-19 11:22:14.547726-08	additional information for report header		f	12	freeText	siteInformation.instruction.headerInfo	0	\N	\N	\N	siteInfo.additional.site.info	\N
32	showValidationFailureIcon	2011-11-08 11:48:47.778057-08	If the analysis has failed validation show icon on results page	true	f	9	boolean	instructions.results.validationFailIcon	0	\N	\N	\N	siteInfo.showValidationFailureIcon	\N
61	external orders	2013-04-17 10:44:01.423189-07	Allow external sites to send electronic orders	false	f	10	boolean	instructions.sampleEntry.acceptOrders	0	\N	\N	\N	siteInfo.external.orders	\N
62	non-conformity signature	2013-05-03 17:13:35.001844-07	Add line for supervisor signature on non-conformity report	false	f	12	boolean	instructions.reports.nonconformity.sig	0	\N	\N	\N	siteInfo.non-conformity.signature	\N
58	headerRightImage	2020-01-22 21:46:35.829492-08	Image for the right side of report header		f	12	logoUpload	siteInfo.instruction.labLogo.right	0	\N	\N	\N	siteInfo.lab.logo.right	\N
42	setFieldForm	2012-03-16 14:09:50.468456-07	set form fields for each different implementation, ex: Haiti	CI_GENERAL	f	11	text	\N	0	\N	\N	\N	siteInfo.setFieldForm	\N
37	stringContext	2012-03-16 14:09:50.468456-07	The context for the property, ex: Cote d' Iviore	CI	f	11	text	\N	0	\N	\N	\N	siteInfo.stringContext	\N
75	siteNumber	2014-06-26 17:19:24.390898-07	The site number of the this lab	12345	f	1	text	\N	0	\N	\N	\N	siteInfo.siteNumber	\N
40	acessionFormat	2012-03-16 14:09:50.468456-07	specifies the format of the acession number,ex: SiteYearNum	SiteYearNum	f	11	text	\N	0	\N	\N	\N	siteInfo.acessionFormat	\N
64	results on workplan	2013-05-20 17:20:43.255016-07	Should there be a space for results on workplan	false	f	13	boolean	instructions.workplan.result	0	\N	\N	\N	siteInfo.results.on.workplan	\N
63	auto-fill collection date/time	2013-05-20 17:20:43.217473-07	Should the specimen collection time and date be autofilled	false	f	10	boolean	instructions.sampleEntry.autofillColl	0	\N	\N	\N	siteInfo.auto-fill.collection.date/time	\N
65	Reception as unit	2013-05-30 16:32:52.211815-07	Should reception be a place where a non-conformity is reported	true	f	14	boolean	instructions.nonconformnity.reception	0	\N	\N	\N	siteInfo.Reception.as.unit	\N
66	Collection as unit	2013-05-30 16:32:52.211815-07	Should sample collection be a place where a non-conformity is reported	true	f	14	boolean	instructions.nonconformity.collection	0	\N	\N	\N	siteInfo.Collection.as.unit	\N
71	validate phone format	2013-08-29 12:35:55.575762-07	Should the format for a phone number be validated	true	f	1	boolean	instructions.site.phone.validation	0	\N	\N	\N	siteInfo.validate.phone.format	\N
70	phone format	2013-08-29 12:35:55.536304-07	The acceptable format for a phone number	+225-xx-xx-xx-xx	f	1	text	instructions.site.phone	0	\N	numericOnly	\N	siteInfo.phone.format	\N
72	Allow duplicate subject number	2013-10-10 16:52:59.667094-07	Allow more than one patient to have same subject number	true	f	15	boolean	instructions.patient.duplicate.subject	0	\N	\N	\N	siteInfo.Allow.duplicate.subject.number	\N
73	validateTechnicalRejection	2014-03-26 12:27:07.798105-07	If true and a technician rejects test results then they are ready to be validated	true	f	9	boolean	instructions.validation.techRejection	0	\N	\N	\N	siteInfo.validateTechnicalRejection	\N
69	validationOnlyNotesAreExternal	2013-08-07 09:27:14.555534-07	If true note are internal except validation	true	f	11	boolean	\N	0	\N	\N	\N	siteInfo.validationOnlyNotesAreExternal	\N
74	allowResultRejection	2014-05-01 20:40:55.94079-07	If true then a technician has the ability to reject an individual test and select a reason for rejection	false	f	9	boolean	allow.result.rejections	0	\N	\N	\N	siteInfo.allowResultRejection	\N
77	augmentTestNameWithType	2020-01-22 21:46:11.009693-08	If true then in certain places the test name will have the the test type	true	f	11	boolean	\N	0	\N	\N	\N	siteInfo.augmentTestNameWithType	\N
78	billingRefNumber	2020-01-22 21:46:11.017212-08	Number used to track billing	false	f	10	boolean	instructions.sample.entry.billing.ref	0	\N	\N	\N	siteInfo.billingRefNumber	\N
79	billingRefNumberLocalization	2020-01-22 21:46:11.017212-08	Term used to for billing reference field	1	f	10	text	instructions.sampleEntry.bill.ref.label	0	\N	localization	\N	siteInfo.billingRefNumberLocalization	\N
80	programs	2020-01-22 21:46:11.034118-08	Management of assigning orders to lab programs		f	11	complex	\N	0	\N	programConfiguration	\N	siteInfo.programs	\N
82	bannerHeading	2020-01-22 21:46:11.082808-08	Text for banner	2	f	1	text	instructions.bannerHeading	0	\N	localization	\N	siteInfo.bannerHeading	\N
83	24 hour clock	2020-01-22 21:46:11.090123-08	12 vs. 24 hour clock	true	f	1	boolean	instructions.clock	0	\N	\N	\N	siteInfo.clock	\N
55	sortQaEvents	2012-10-24 11:58:24.708055-07	sort qa events in lists	false	f	14	boolean	siteInformation.instruction.sortQaEvents	0	\N	\N	\N	siteInfo.sortQaEvents	\N
81	Program	2020-01-22 21:46:11.060406-08	Is a program part of an order	true	f	10	boolean	instructions.sample.entry.program	0	\N	\N	\N	siteInfo.Program	\N
85	supportPatientNationality	2020-01-22 21:46:35.916872-08	Should the nationality of the patient be specified	true	f	15	boolean	siteInfo.instruction.patient.nationality	0	\N	\N	\N	siteInfo.patient.nationality	\N
86	restrictFreeTextRefSiteEntry	2020-01-22 21:46:35.930856-08	Users cannot enter new referring sites through sample entry	false	f	10	boolean	instructions.sample.entry.limit.ref.site	0	\N	\N	\N	siteInfo.restrictFreeTextRefSiteEntry	\N
84	headerLeftImage	2020-01-22 21:46:35.829492-08	Image for the left side of report header	1	f	12	logoUpload	siteInfo.instruction.labLogo.left	0	\N	\N	\N	siteInfo.lab.logo.left	\N
93	Subject number required	2020-01-22 21:46:36.103525-08	Is a subject number required for patient	false	f	15	boolean	instruction.patient.subNumber.required	0	\N	\N	\N	site.info.patient.subjectNumber.required	\N
94	sample id required	2020-01-22 21:46:36.103525-08	Is an requester sample id required	true	f	14	boolean	instruction.nonConformity.sample.id	0	\N	\N	\N	site.info.nonConformity.sample.id	\N
92	Patient ID required	2020-01-22 21:46:36.103525-08	Is an id required for the patient	false	f	15	boolean	instruction.patient.id.required	0	\N	\N	\N	site.info.patient.id.required	\N
95	Patient management tab	2020-01-22 21:46:36.136751-08	If true, display patient tab	true	f	17	boolean	instruction.tabs.patientIdentity	0	\N	\N	\N	site.info.tabs.patientIdentity	\N
51	configuration name	2020-01-22 21:46:11.07539-08	The name which will appear after the version number in header	CI_GENERAL	f	11	text	\N	0	\N	\N	\N	siteInfo.configuration.name	\N
76	Accession number prefix	2014-06-26 17:19:24.393002-07	Prefix for SITEYEARNUM format.  Can not be changed if there are samples	12345	f	1	text	\N	0	\N	\N	\N	siteInfo.Accession.number.prefix	\N
10	TrainingInstallation	2020-01-22 21:46:42.276023-08	Allows for deletion of all patient and sample data	false	f	1	boolean	\N	0	\N	\N	\N	siteInfo.TrainingInstallation	\N
87	new born	2020-01-22 21:46:36.093664-08	max age of a new born	1	f	16	text	instrution.result.age	0	\N	\N	\N		site.info.result.age.newborn
88	infant	2020-01-22 21:46:36.093664-08	max age of an infant	12	f	16	text	instrution.result.age	0	\N	\N	\N		site.info.result.age.infant
89	young child	2020-01-22 21:46:36.093664-08	max age of a young child	60	f	16	text	instrution.result.age	0	\N	\N	\N		site.info.result.age.youngchild
90	child	2020-01-22 21:46:36.093664-08	max age of a child	168	f	16	text	instrution.result.age	0	\N	\N	\N		site.info.result.age.child
91	adult	2020-01-22 21:46:36.093664-08	max age of an adult	Infinity	f	16	text	instrution.result.age	0	\N	\N	\N		site.info.result.age.adult
96	Study Management tab	2020-01-22 21:46:41.443156-08	If true, display study tab	true	f	17	boolean	instruction.tabs.studyIdentity	0	\N	\N	\N	site.info.tabs.studyIdentity	\N
41	passwordRequirements	2012-03-16 14:09:50.468456-07	changes the password requirements depending on site, ex: HAITI	CDI	f	11	text	\N	0	\N	\N	\N	siteInfo.passwordRequirements	\N
97	Data Sub URL	2020-01-22 21:46:54.218304-08	The url where data is submitted to the VL Dashboard		f	\N	text	\N	0	\N	\N	\N	\N	\N
98	Data Submission	2020-01-22 21:46:54.235651-08	Show Option to submit data to VL DASH	true	f	17	boolean	\N	0	\N	\N	\N	siteInfo.dataSubmission	\N
99	Non Conformity tab	2020-01-22 21:46:56.144835-08	If true, display Non Conformity tab	true	f	17	boolean	\N	0	\N	\N	\N	site.info.tabs.nonconformity	\N
\.


--
-- Data for Name: site_information_domain; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.site_information_domain (id, name, description) FROM stdin;
1	siteIdentity	Identityfing items which don't change the behavior
2	patientSharing	Items needed to share patient information
3	siteExtras	Items which turn extra capacity on and off
4	formating	Items which specify the format of artifacts
5	rules	Items which change the busness rules and effect the workflow
6	testUsage	Items which change the busness rules and effect the workflow
9	resultConfiguration	site information which effects the handling of results
10	sampleEntryConfig	Configuration for those items which can appear on the sample entry form
11	hiddenProperties	Configuration properties invisible to the user
8	resultReporting	Items which effect reports being sent electronically
12	printedReportsConfig	items which effect printed reports
13	workplanConfig	Items having to do with the configuration of workplans
14	non_conformityConfig	Items which have to do with configuration of non-conformity page
15	patientEntryConfig	Configuration items for patient entry
16	resultAgeRange	The maximum age range in months
17	MenuStatementConfig	Manage menu Item
18	validationConfig	Items that have to do with common field validation
\.


--
-- Name: site_information_domain_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.site_information_domain_seq', 18, true);


--
-- Name: site_information_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.site_information_seq', 104, true);


--
-- Data for Name: source_of_sample; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.source_of_sample (id, description, domain, lastupdated) FROM stdin;
43	Ankle	H	2006-09-12 10:09:25
56	Bone marrow	H	2006-09-12 10:14:26
62	Brain meninges	H	2006-09-12 10:24:32
88	Eye lid	H	2006-09-12 10:34:28
91	Finger 1st	H	2006-09-12 10:35:08
102	Head	H	2006-09-12 10:38:14
109	Lingula	H	2006-09-12 10:39:28
205	test11	A	2006-12-11 14:48:24
26	Left Lower Lobe	H	2006-09-12 10:03:04
29	Right Lower Lobe	H	2006-09-12 10:03:24
30	Middle Lobe	H	2006-09-12 10:03:37
31	Acute	H	2006-09-12 10:05:06
32	Convalescent	H	2006-09-12 10:05:19
33	Abdominal	H	2006-11-08 11:10:05.484
34	Amniotic	H	2006-09-12 10:05:53
35	Joint	H	2006-09-12 10:06:04
36	Paracentesis	H	2006-09-12 10:07:23
37	Pericardial	H	2006-09-12 10:07:33
38	Synovial	H	2006-09-12 10:07:43
39	Thoracentesis	H	2006-09-12 10:08:09
40	Vitreous	H	2006-09-12 10:08:20
41	Abdomen	H	2006-11-08 11:09:50.202
42	Adenoid	H	2006-09-12 10:09:17
44	Aorta	H	2006-09-12 10:09:34
45	Arm	H	2006-09-12 10:09:42
46	Axilla	H	2006-09-12 10:09:53
47	Back	H	2006-09-12 10:10:00
48	Bladder	H	2006-09-12 10:10:36
49	Bone	A	2006-11-27 09:21:04
50	Bone tibia	H	2006-09-12 10:13:00
51	Bone femur	H	2006-09-12 10:12:33
52	Bone coccyx	H	2006-09-12 10:13:18
53	Bone clavicle	H	2006-09-12 10:13:32
54	Bone cranium	H	2006-09-12 10:13:46
55	Bone mastoid	H	2006-09-12 10:14:01
57	Bowel	H	2006-09-12 10:14:37
58	Brain	H	2006-09-12 10:23:28
59	Brain frontal lobe	H	2006-09-12 10:23:48
60	Brain subgaleal	H	2006-09-12 10:24:07
61	Brain subdural	H	2006-09-12 10:24:21
63	Brain parietal lobe	H	2006-09-12 10:24:46
64	Brain dura	H	2006-09-12 10:24:56
65	Brain cerebellum	H	2006-09-12 10:25:10
66	Breast	H	2006-09-12 10:25:19
67	Buccal	H	2006-09-12 10:25:37
68	Buttock	H	2006-09-12 10:26:10
69	Calf	H	2006-09-12 10:26:19
70	Cervix	H	2006-09-12 10:26:47
71	Chest	H	2006-09-12 10:26:56
72	Coccyx	H	2006-09-12 10:27:19
73	Colon	H	2006-09-12 10:27:27
74	Composite	H	2006-09-12 10:27:35
75	Disc	H	2006-09-12 10:27:47
76	Duodenum	H	2006-09-12 10:27:58
77	Ear	H	2006-09-12 10:28:05
78	Ear canal	H	2006-09-12 10:28:20
79	Ear mastoid	H	2006-09-12 10:28:32
80	Elbow	H	2006-09-12 10:28:43
81	Endometrium	H	2006-09-12 10:28:53
82	Epidural	H	2006-09-12 10:29:03
83	Epiglottis	H	2006-09-12 10:29:17
84	Esophagus	H	2006-09-12 10:29:37
85	Eye	H	2006-09-12 10:33:46
86	Eye cornea	H	2006-09-12 10:34:00
87	Eye conjunctiva	H	2006-09-12 10:34:19
89	Face	H	2006-09-12 10:34:40
90	Finger	H	2006-09-12 10:34:48
92	Finger 2nd	H	2006-09-12 10:35:19
93	Finger 3rd	H	2006-09-12 10:35:28
94	Finger 4th	H	2006-09-12 10:35:36
95	Finger thumb	H	2006-09-12 10:35:47
96	Foot	H	2006-09-12 10:36:00
97	Gall Bladder	H	2006-09-12 10:37:27
98	Gastric	H	2006-09-12 10:37:35
99	Hair	H	2006-09-12 10:37:42
100	Hand	H	2006-09-12 10:37:52
101	Hard Palate	H	2006-09-12 10:38:03
103	Hip	H	2006-09-12 10:38:25
104	Intestine	H	2006-09-12 10:38:38
105	Jaw	H	2006-09-12 10:38:48
106	Kidney	H	2006-09-12 10:38:58
107	Knee	H	2006-09-12 10:39:07
108	Leg	H	2006-09-12 10:39:20
110	Lip	H	2006-09-12 10:39:43
111	Lumbar	H	2006-09-12 10:39:51
112	Lumbar Disc Space	H	2006-09-12 10:40:03
113	Lymph Node	H	2006-09-12 10:40:14
114	Lymph Node abdominal	H	2006-09-12 10:40:36
115	Lymph Node axillary	H	2006-09-12 10:40:50
116	Lymph Node cervical	H	2006-09-12 10:41:04
117	Lymph Node hilar	H	2006-09-12 10:41:22
118	Lymph Node inguinal	H	2006-09-12 10:41:47
124	Neck	H	2006-09-12 11:34:26
125	Nose	H	2006-09-12 11:34:34
140	Rectum	H	2006-09-12 11:38:12
143	Scrotum	H	2006-09-12 11:39:22
144	Shin	H	2006-09-12 11:39:30
174	Wrist	H	2006-09-12 11:46:47
185	Heart	H	2006-09-12 11:58:15
189	Lymph Node mediastinal	H	2006-09-12 12:45:59
190	Lymph Node mesenteric	H	2006-09-12 12:46:17
191	Lymph Node paratracheal	H	2006-09-12 12:46:30
192	Lymph Node portal	H	2006-09-12 12:47:25
193	Lymph Node post auricular	H	2006-09-12 12:47:42
194	Lymph Node submandibular	H	2006-09-12 12:47:57
195	Lymph Node supraclavicular	H	2006-09-12 12:48:23
196	Lymph Node tracheal	H	2006-09-12 12:48:33
197	Port-a-Cath	H	2006-09-13 08:49:45
198	IV Catheter Tip	H	2006-09-13 08:50:14
120	Urethral	H	2006-09-12 11:33:15
28	Right Upper Lobe	H	2006-09-12 10:03:14
119	Cervical	H	2006-09-12 11:33:05
121	Oral	H	2006-09-12 11:33:38
122	Mandible	H	2006-09-12 11:34:14
123	Nail	H	2006-09-12 11:34:20
126	Nose anterior nares	H	2006-09-12 11:34:59
127	Nose nares	H	2006-09-12 11:35:12
128	Nose turbinate	H	2006-09-12 11:35:22
129	Omentum	H	2006-09-12 11:35:33
130	Paraspinal	H	2006-09-12 11:35:44
131	Paratracheal	H	2006-09-12 11:36:11
132	Parotid	H	2006-09-12 11:36:21
133	Penis	H	2006-09-12 11:36:59
134	Pericardium	H	2006-09-12 11:37:09
135	Perineum	H	2006-09-12 11:37:22
136	Peritoneum	H	2006-09-12 11:37:37
137	Placenta	H	2006-09-12 11:37:45
138	Pleura	H	2006-09-12 11:37:56
139	Prostate	H	2006-09-12 11:38:04
141	Sacrum	H	2006-09-12 11:38:26
142	Scalp	H	2006-09-12 11:38:36
145	Shoulder	H	2006-09-12 11:39:38
146	Sinus	H	2006-09-12 11:39:49
147	Sinus ethmoid	H	2006-09-12 11:40:08
148	Sinus frontal	H	2006-09-12 11:40:17
149	Sinus maxillary	H	2006-09-12 11:41:30
150	Sinus sphenoid	H	2006-09-12 11:41:42
151	Skin	H	2006-09-12 11:41:55
152	Small Bowel	H	2006-09-12 11:42:04
153	Spine	H	2006-09-12 11:42:11
154	Spleen	H	2006-09-12 11:42:20
155	Sternum	H	2006-09-12 11:42:30
156	Stomach	H	2006-09-12 11:42:38
157	Synovium	H	2006-09-12 11:43:00
158	Testicle	H	2006-09-12 11:43:17
159	Thigh	H	2006-09-12 11:43:25
160	Toe	H	2006-09-12 11:43:33
161	Toe great	H	2006-09-12 11:43:58
162	Toe 1st	H	2006-09-12 11:44:10
163	Toe 2nd	H	2006-09-12 11:44:19
164	Toe 3rd	H	2006-09-12 11:44:29
165	Toe 4th	H	2006-09-12 11:44:38
166	Toe 5th	H	2006-09-12 11:45:37
167	Toenail	H	2006-09-12 11:45:48
168	Tongue	H	2006-09-12 11:45:57
169	Tonsil	H	2006-09-12 11:46:04
170	Vagina	H	2006-09-12 11:46:18
171	Vein	H	2006-09-12 11:46:25
172	Vertebra	H	2006-09-12 11:46:32
173	Vulva	H	2006-09-12 11:46:40
175	Other	H	2006-09-12 11:47:35
176	Midstream	H	2006-09-12 11:47:54
177	Clean Catch	H	2006-09-12 11:48:05
178	Catheter	H	2006-09-12 11:48:12
179	Foley Catheter	H	2006-09-12 11:48:33
180	Peripheral	H	2006-09-12 11:48:45
181	EDTA	H	2006-09-12 11:48:51
182	Whole	H	2006-09-12 11:48:57
183	Venous	H	2006-09-12 11:49:06
184	Cord	H	2006-09-12 11:49:11
186	Heart valve	H	2006-09-12 11:58:28
187	Heart aortic	H	2006-09-12 11:58:38
188	Heart pericardium	H	2006-09-12 11:58:49
25	Left Upper Lobe 	H	2006-09-12 10:02:44
\.


--
-- Name: source_of_sample_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.source_of_sample_seq', 1, false);


--
-- Data for Name: state_code; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.state_code (id, code, description, lastupdated) FROM stdin;
17	MB	Manitoba	2006-08-11 14:14:02
18	NB	New Brunswick	2006-08-11 14:14:02
19	NL	Newfoundland and Labrador	2006-08-11 14:14:02
20	NT	Northwest Territories	2006-08-11 14:14:02
21	NS	Nova Scotia	2006-08-11 14:14:02
22	NU	Nunavut	2006-08-11 14:14:02
23	ON	Ontario	2006-08-11 14:14:02
24	PE	Prince Edward Island	2006-08-11 14:14:02
25	QC	Quebec	2006-08-11 14:14:02
15	AA	Alberta	2006-11-08 11:10:52.625
16	BC	British Columbia	2006-08-11 14:14:02
26	SK	Saskatchewan	2006-08-11 14:14:02
27	YU	Yukon	2006-08-11 14:14:02
30	AL	ALABAMA             	2006-08-11 14:14:02
31	AK	ALASKA              	2006-08-11 14:14:02
32	AZ	ARIZONA             	2006-08-11 14:14:02
33	AR	ARKANSAS            	2006-08-11 14:14:02
34	CA	CALIFORNIA          	2006-08-11 14:14:02
35	CO	COLORADO            	2006-08-11 14:14:02
36	CT	CONNECTICUT         	2006-08-11 14:14:02
37	DE	DELAWARE            	2006-08-11 14:14:02
38	DC	DISTRICT OF COLUMBIA	2006-08-11 14:14:02
39	FL	FLORIDA             	2006-08-11 14:14:02
40	GA	GEORGIA             	2006-08-11 14:14:02
41	HI	HAWAII              	2006-08-11 14:14:02
42	ID	IDAHO               	2006-08-11 14:14:02
43	IL	ILLINOIS            	2006-08-11 14:14:02
44	IN	INDIANA             	2006-08-11 14:14:02
45	IA	IOWA                	2006-08-11 14:14:02
46	KS	KANSAS              	2006-08-11 14:14:02
47	KY	KENTUCKY            	2006-08-11 14:14:02
48	LA	LOUISIANA           	2006-08-11 14:14:02
49	ME	MAINE               	2006-08-11 14:14:02
50	MD	MARYLAND            	2006-08-11 14:14:02
51	MA	MASSACHUSETTS       	2006-08-11 14:14:02
52	MI	MICHIGAN            	2006-08-11 14:14:02
53	MN	MINNESOTA           	2006-08-11 14:14:02
54	MS	MISSISSIPPI         	2006-08-11 14:14:02
55	MO	MISSOURI            	2006-08-11 14:14:02
56	MT	MONTANA             	2006-08-11 14:14:02
57	NE	NEBRASKA            	2006-08-11 14:14:02
58	NV	NEVADA              	2006-08-11 14:14:02
59	NH	NEW HAMPSHIRE       	2006-08-11 14:14:02
60	NJ	NEW JERSEY          	2006-08-11 14:14:02
61	NM	NEW MEXICO          	2006-08-11 14:14:02
62	NY	NEW YORK            	2006-08-11 14:14:02
63	NC	NORTH CAROLINA      	2006-08-11 14:14:02
64	ND	NORTH DAKOTA        	2006-08-11 14:14:02
65	OH	OHIO                	2006-08-11 14:14:02
66	OK	OKLAHOMA            	2006-08-11 14:14:02
67	OR	OREGON              	2006-08-11 14:14:02
68	PA	PENNSYLVANIA        	2006-08-11 14:14:02
69	RI	RHODE ISLAND        	2006-08-11 14:14:02
70	SC	SOUTH CAROLINA      	2006-08-11 14:14:02
71	SD	SOUTH DAKOTA        	2006-08-11 14:14:02
72	TN	TENNESSEE           	2006-08-11 14:14:02
73	TX	TEXAS               	2006-08-11 14:14:02
74	UT	UTAH                	2006-08-11 14:14:02
75	VT	VERMONT             	2006-08-11 14:14:02
76	VA	VIRGINIA            	2006-08-11 14:14:02
77	WA	WASHINGTON          	2006-08-11 14:14:02
78	WV	WEST VIRGINIA       	2006-08-11 14:14:02
79	WI	WISCONSIN           	2006-08-11 14:14:02
80	WY	WYOMING             	2006-08-11 14:14:02
85	CF	Confusion	2006-08-30 15:49:37
82	AB	new test update	2006-08-11 14:14:02
\.


--
-- Name: state_code_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.state_code_seq', 1, false);


--
-- Data for Name: status_of_sample; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.status_of_sample (id, description, code, status_type, lastupdated, name, display_key, is_active) FROM stdin;
4	This test has not yet been done	1	ANALYSIS	2011-02-16 12:45:57.196561	Not Tested	status.test.notStarted	Y
7	The Biologist did not accept this result as valid	1	ANALYSIS	2011-11-08 11:48:51.093674	Biologist Rejection	status.test.biologist.reject	Y
14	Test was requested but then canceled	1	ANALYSIS	2011-11-08 11:48:51.093674	Test Canceled	status.test.canceled	Y
15	The results of the test were accepted by technician as being valid	1	ANALYSIS	2011-11-08 11:48:51.093674	Technical Acceptance	status.test.tech.accepted	Y
16	The results of the test were not accepted by the technicain	1	ANALYSIS	2011-11-08 11:48:51.093674	Technical Rejected	status.test.tech.rejected	Y
6	The results of the analysis are final	1	ANALYSIS	2011-02-16 12:45:57.196561	Finalized	status.test.valid	Y
1	No tests have been run for this order	1	ORDER	2011-02-16 12:45:57.196561	Test Entered	status.sample.notStarted	Y
2	Some tests have been run on this order	1	ORDER	2011-02-16 12:45:57.196561	Testing Started	status.sample.started	Y
3	All tests have been run on this order	1	ORDER	2011-02-16 12:45:57.196561	Testing finished	status.sample.finished	Y
19	The sample has been canceled by the user	1	SAMPLE	2012-05-14 11:24:08.768263	SampleCanceled	status.sample.entered	Y
20	The sample has been entered into the system	1	SAMPLE	2012-05-14 11:24:08.768263	SampleEntered	status.sample.entered	Y
12	The order is non-conforming	1	ORDER	2011-08-23 09:30:01.227121	NonConforming	status.sample.nonConforming	N
13	The order is non-conforming	1	ANALYSIS	2011-08-23 09:30:01.227121	NonConforming	status.analysis.nonConforming	N
21	The electronic order has been entered into OE	1	EXTERNAL_ORDER	2013-04-10 16:00:05.19233	Entered	status.sample.entered	Y
22	The electronic order has been cancelled	1	EXTERNAL_ORDER	2013-04-10 16:00:05.19233	Cancelled	status.sample.cancelled	Y
23	The patient associated with the electronic order has appeared at the lab	1	EXTERNAL_ORDER	2013-04-10 16:00:05.19233	Realized	status.sample.realized	Y
\.


--
-- Name: status_of_sample_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.status_of_sample_seq', 23, true);


--
-- Data for Name: storage_location; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.storage_location (id, sort_order, name, location, is_available, parent_storageloc_id, storage_unit_id) FROM stdin;
\.


--
-- Data for Name: storage_unit; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.storage_unit (id, category, description, is_singular) FROM stdin;
\.


--
-- Data for Name: system_module; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_module (id, name, description, has_select_flag, has_add_flag, has_update_flag, has_delete_flag) FROM stdin;
1	PanelItem	Master Lists => Panel Item => edit	Y	Y	Y	N
2	DictionaryCategory	Master Lists => Dictionary Category => edit	Y	Y	Y	N
3	Dictionary	Master Lists => Dictonary => edit	Y	Y	Y	N
4	Gender	Master Lists => Gender => edit	Y	Y	Y	N
5	LoginUser	Master Lists => Login User => Edit	Y	Y	Y	N
6	Organization	Master Lists => Organization => edit	Y	Y	Y	N
7	Panel	Master Lists => Panel	Y	Y	Y	Y
8	PatientResults	Results->By Patient	N	N	N	N
9	ResultLimits	Master Lists => ResultLimits => edit	Y	Y	Y	N
10	Result	Master Lists => Result => edit	Y	Y	Y	N
11	Role	Master Lists => Role => edit	Y	Y	Y	N
12	StatusOfSample	Master Lists => StatusOfSample => edit	Y	Y	Y	N
13	SystemModule	Master Lists => System Module	Y	Y	Y	N
14	SystemUser	Master Lists => System User	Y	Y	Y	N
15	Test	Master Lists => Test	Y	Y	Y	N
16	SampleEntry	Sample->Sample Entry	N	N	N	N
17	MasterList	Administration	N	N	N	N
18	Inventory	Inventory	N	N	N	N
26	StatusResults	Results->By Status	N	N	N	N
27	ReportAdmin	Master Lists => OR Admin	Y	Y	Y	N
28	ReportUserDetail	Reports	Y	Y	Y	N
29	ReportUserOption	Reports	Y	Y	Y	N
30	ReportUserRun	Reports	Y	Y	Y	N
31	TypeOfTestResult	Master Lists => Type Of Test Result	Y	Y	Y	N
32	SystemUserModule	Master Lists => System User Module	Y	Y	Y	N
33	ResultsEntry	Result Management => Results Entry	Y	Y	Y	N
34	TestSection	Master Lists => Test Section	Y	Y	Y	N
35	TypeOfSample	Master Lists => Type Of Sample	Y	Y	Y	N
36	UnitOfMeasure	Master Lists => Unit Of Measure	N	N	N	N
40	UserRole	MasterList => UserRole	Y	Y	Y	Y
41	PatientType	MasterList => PatientType	Y	Y	Y	Y
42	TypeOfSamplePanel	MasterList => Associtate type of sample with panel	Y	Y	Y	Y
43	TypeOfSampleTest	MasterList => Associtate type of sample with tests	Y	Y	Y	Y
44	UnifiedSystemUser	MasterList->ManageUsers	Y	Y	Y	Y
45	LogbookResults	Results=>logbook=>save	Y	Y	Y	Y
46	SamplePatientEntry	Sample->SampleEntry	Y	Y	Y	Y
47	SiteInformation	MasterList=>Site Information	Y	Y	Y	Y
48	AnalyzerTestName	MasterList->Analyzer Test Name	Y	Y	Y	Y
49	AnalyzerResults	Results->Analyzers	Y	Y	Y	Y
51	SampleEntryByProject:initial	Sample=>CreateSample=>initial	Y	Y	Y	Y
52	SampleEntryByProject:verify	Sample=>CreateSample=>verify	Y	Y	Y	Y
55	PatientEntryByProject:initial	Patient=>Enter=>initial	Y	Y	Y	Y
56	PatientEntryByProject:verify	Patient=>Enter=>verify	Y	Y	Y	Y
60	LogbookResults:serology	Results=>Enter=>serology	Y	Y	Y	Y
63	AccessionResults	Results=>Search=>Lab No.	Y	Y	Y	Y
65	AnalyzerResults:cobas_integra	Results=>Analyzer=>cobas_integra	Y	Y	Y	Y
66	AnalyzerResults:sysmex	Results=>Analyzer=>sysmex	Y	Y	Y	Y
67	AnalyzerResults:facscalibur	Results=>Analyzer=>facscalibur	Y	Y	Y	Y
68	AnalyzerResults:evolis	Results=>Analyzer=>evolis	Y	Y	Y	Y
69	Workplan:test	Workplan=>test	Y	Y	Y	Y
84	TestResult	Admin=>TestResult	Y	Y	Y	Y
86	TestReflex	Admin=>TestReflex	Y	Y	Y	Y
87	TestAnalyte	Admin=>TestAnalyte	Y	Y	Y	Y
97	Method	Admin=>Method	Y	Y	Y	Y
53	SampleEditByProject:readwrite	Sample=>SampleEditByProject:readwrite	Y	Y	Y	Y
54	SampleEditByProject:readonly	Sample=>SampleEditByProject:readonly	Y	Y	Y	Y
20	LogbookResults:chem	Results->By Logbook->Chem	N	N	N	N
25	LogbookResults:HIV	Results->By Logbook->VCT	N	N	N	N
19	LogbookResults:bacteriology	Results->By Logbook->Bacteria	N	N	N	N
21	LogbookResults:ECBU	Results->By Logbook->ECBU	N	N	N	N
23	LogbookResults:immuno	Results->By Logbook->Immuno	N	N	N	N
101	Analyte	Admin=>Analyte	Y	Y	Y	Y
106	SampleEdit	Sample=>edit	Y	Y	Y	Y
107	NonConformity	NonConformity	Y	Y	Y	Y
108	Report:patient	Patient reports	Y	Y	Y	Y
109	Report:summary	Lab summary reports	Y	Y	Y	Y
110	Report:indicator	Lab quality indicator reports	Y	Y	Y	Y
111	ResultValidation:serology	Validation=>serology	Y	Y	Y	Y
115	ResultValidation:virology	Validation=>virology	Y	Y	Y	Y
116	PatientEditByProject:readwrite	Patient=>PatientEdit	Y	Y	Y	Y
117	PatientEditByProject:readonly	Patient=>PatientConsult	Y	Y	Y	Y
118	SampleEdit:readwrite	Sample -> edit	Y	Y	Y	Y
123	SampleEdit:readonly	Sample=>SampleConsult	Y	Y	Y	Y
173	ReportUserDetail:patientARV2	Report=>patient=>ARV follow-up Save	Y	Y	Y	Y
174	ReportUserDetail:patientEID	Report=>patient=>EID	Y	Y	Y	Y
185	ReferredOutTests	Results=>Referrals	Y	Y	Y	Y
253	SampleConfirmationEntry	Sample=>sample confirmation	Y	Y	Y	Y
322	LogbookResults:mycobacteriology	Results=>logbooks=>mycobacteriology	Y	Y	Y	Y
561	Workplan:panel	Workplan=>panel	Y	Y	Y	Y
393	LogbookResults:mycrobacteriology	Results=>logbooks=>mycrobacteriology	Y	Y	Y	Y
72	Workplan:Hematology	Workplan=>Hematology	Y	Y	Y	Y
70	Workplan:Serology	Workplan=>Serology	Y	Y	Y	Y
74	Workplan:Virologie	Workplan=>Virologie	Y	Y	Y	Y
61	LogbookResults:Virologie	LogbookResults=>Enter=>Virologie	Y	Y	Y	Y
24	LogbookResults:Parasitology	Results->By Logbook->Parasitology	N	N	N	N
1506	SampleBatchEntryOnDemand		Y	Y	Y	Y
1507	SampleBatchEntryPrePrinted		Y	Y	Y	Y
1508	PrintBarcode		Y	Y	Y	Y
1509	TestSectionRenameEntry		Y	Y	Y	Y
1510	UnitOfMeasureRenameEntry		Y	Y	Y	Y
1511	TestAdd		Y	Y	Y	Y
1512	PanelManagement		Y	Y	Y	Y
394	LogbookResults:cytobacteriology	Results=>logbooks=>cytobacteriology	Y	Y	Y	Y
396	LogbookResults:liquidBio	Results=>logbooks=>liquidBio	Y	Y	Y	Y
397	LogbookResults:endocrin	Results=>logbooks=>endocrin	Y	Y	Y	Y
341	AnalyzerResults:facscanto	Results=>Analyzer=>facscanto	Y	Y	Y	Y
657	LogbookResults:serologie	Results=>Enter=>serologie	Y	Y	Y	Y
507	Workplan:bacteriology	Workplan=>bacteriology	Y	Y	Y	Y
424	Workplan:chem	Workplan=>chem	Y	Y	Y	Y
425	Workplan:cytobacteriology	Workplan=>cytobacteriology	Y	Y	Y	Y
426	Workplan:ECBU	Workplan=>ECBU	Y	Y	Y	Y
428	Workplan:immuno	Workplan=>immuno	Y	Y	Y	Y
429	Workplan:HIV	Workplan=>HIV	Y	Y	Y	Y
431	Workplan:liquidBio	Workplan=>liquidBio	Y	Y	Y	Y
432	Workplan:mycrobacteriology	Workplan=>mycrobacteriology	Y	Y	Y	Y
433	Workplan:endocrin	Workplan=>endocrin	Y	Y	Y	Y
434	Workplan:serologie	Workplan=>serologie	Y	Y	Y	Y
644	Workplan:mycology	Workplan=>mycology	Y	Y	Y	Y
645	LogbookResults:mycology	LogbookResults=>mycology	Y	Y	Y	Y
816	Workplan:hemato-immunology	workplan=>units=>hemato-immunology	Y	Y	Y	Y
1378	AuditTrailView	Report=>view audit log	Y	Y	Y	Y
1379	AnalyzerResults:cobasDBS	Result=>analyzers=>CobasTaqmanDBS	Y	Y	Y	Y
1180	LogbookResults:hemato-immunology	LogbookResults=>hemato-immunology	Y	Y	Y	Y
1184	ResultValidation:Hemto-Immunology	validation=>units=>Hemato-Immunity	Y	Y	Y	Y
1281	PatientDataOnResults	Able to view patient data when looking at results	Y	Y	Y	Y
1376	ResultValidation	validation return	Y	Y	Y	Y
1381	ResultValidation:Cytobacteriologie	ResultValidation=>Cytobacteriologie	Y	Y	Y	Y
1382	ResultValidation:ECBU	ResultValidation=>ECBU	Y	Y	Y	Y
1383	ResultValidation:Parasitology	ResultValidation=>Parasitology	Y	Y	Y	Y
1384	ResultValidation:Liquides biologique	ResultValidation=>Liquides biologique	Y	Y	Y	Y
1385	ResultValidation:Mycobacteriology	ResultValidation=>Mycobacteriology	Y	Y	Y	Y
1386	ResultValidation:Endocrinologie	ResultValidation=>Endocrinologie	Y	Y	Y	Y
1387	ResultValidation:Serologie	ResultValidation=>Serologie	Y	Y	Y	Y
1388	ResultValidation:VCT	ResultValidation=>VCT	Y	Y	Y	Y
1390	ResultValidation:Bacteria	ResultValidation=>Bacteria	Y	Y	Y	Y
1391	ResultValidation:mycology	ResultValidation=>mycology	Y	Y	Y	Y
1392	AnalyzerResults:cobasc311	AnalyzerResults=>cobasc311	Y	Y	Y	Y
1393	SampleEntryConfig	Admin=>Sample entry config	Y	Y	Y	Y
1394	WorkplanConfiguration	Admin=>Workplan configuration	Y	Y	Y	Y
1395	TestManagementConfig	Admin=>test config	Y	Y	Y	Y
1396	TestRenameEntry	Admin=>test config=>test rename	Y	Y	Y	Y
1397	PatientConfiguration	Admin=>patient config	Y	Y	Y	Y
1398	ResultConfiguration	Admin=>Result config	Y	Y	Y	Y
1399	PrintedReportsConfiguration	Admin=>printed reports config	Y	Y	Y	Y
1400	NonConformityConfiguration	Admin=>non-conformity config	Y	Y	Y	Y
1401	TestActivation	Admin=>test mang=>activate	Y	Y	Y	Y
1402	Workplan	Workplan=>Enter	Y	Y	Y	Y
1181	LogbookResults:Biochemistry	LogbookResults=>Biochemistry	Y	Y	Y	Y
114	ResultValidation:Biochemistry	ResultValidation=>Biochemistry	Y	Y	Y	Y
73	Workplan:Biochemistry	Workplan=>Biochemistry	Y	Y	Y	Y
22	LogbookResults:Hematology	LogbookResults=>Hematology	N	N	N	N
113	ResultValidation:Hematology	ResultValidation=>Hematology	Y	Y	Y	Y
1178	LogbookResults:Serology-Immunology	LogbookResults=>Serology-Immunology	Y	Y	Y	Y
1186	ResultValidation:Serology-Immunology	ResultValidation=>Serology-Immunology	Y	Y	Y	Y
1183	Workplan:Serology-Immunology	Workplan=>Serology-Immunology	Y	Y	Y	Y
1179	LogbookResults:Immunology	LogbookResults=>Immunology	Y	Y	Y	Y
112	ResultValidation:Immunology	ResultValidation=>Immunology	Y	Y	Y	Y
71	Workplan:Immunology	Workplan=>Immunology	Y	Y	Y	Y
395	LogbookResults:Molecular Biology	LogbookResults=>Molecular Biology	Y	Y	Y	Y
1380	ResultValidation:Molecular Biology	ResultValidation=>Molecular Biology	Y	Y	Y	Y
430	Workplan:Molecular Biology	Workplan=>Molecular Biology	Y	Y	Y	Y
1404	PanelRenameEntry	Admin=>test mang=>panel rename	Y	Y	Y	Y
1405	TestOrderability	Admin=>test mang=>Test Orderability	Y	Y	Y	Y
1406	TestCatalog	Admin=>test mang=>activate	Y	Y	Y	Y
1407	BatchTestReassign	Admin=>batchTestReassignment	Y	Y	Y	Y
1408	TestSectionManagement	Admin=>test manag=>test section	Y	Y	Y	Y
1409	TestSectionOrder	Admin=>test manag=>test section order	Y	Y	Y	Y
1410	TestSectionTestAssign	Admin=>test manag=>test section assign	Y	Y	Y	Y
1411	TestSectionCreate	Admin=>test manag=>test section create	Y	Y	Y	Y
1412	SampleTypeRenameEntry	Admin=>test manag=>type rename	Y	Y	Y	Y
1500	MenuStatementConfig	Admin=>MenuStatementConfig	Y	Y	Y	Y
1501	LogbookResults:Serology	LogbookResults=>Serology	Y	Y	Y	Y
1502	ResultValidation:Serology	Validation=>Serology	Y	Y	Y	Y
1503	ResultValidation:serology	ResultValidation=>serology	Y	Y	Y	Y
1504	ResultValidation:virology	ResultValidation=>virology	Y	Y	Y	Y
1389	ResultValidation:Virologie	ResultValidation=>Virologie	Y	Y	Y	Y
1100	Access.sample.accessionNo.edit	Sample->SampleEntryByProject edit sample number	Y	Y	Y	Y
1101	Access.patient.subjectNos.edit	Patient->PatientEntryByProject edit subject numbers	Y	Y	Y	Y
1420	Workplan:EID	Workplan=>EID	Y	Y	Y	Y
1421	LogbookResults:EID	LogbookResults=>EID	Y	Y	Y	Y
1422	ResultValidation:EID	ResultValidation=>EID	Y	Y	Y	Y
1423	Workplan:VL	Workplan=>VL	Y	Y	Y	Y
1424	LogbookResults:VL	LogbookResults=>VL	Y	Y	Y	Y
1425	ResultValidation:VL	ResultValidation=>VL	Y	Y	Y	Y
427	Workplan:Parasitology	Workplan=>Parasitology	Y	Y	Y	Y
1505	SampleBatchEntry		Y	Y	Y	Y
1513	SampleTypeManagement		Y	Y	Y	Y
1514	UomManagement		Y	Y	Y	Y
1515	UomCreate		Y	Y	Y	Y
1516	SampleTypeCreate		Y	Y	Y	Y
1517	SampleTypeOrder		Y	Y	Y	Y
1518	SampleTypeTestAssign		Y	Y	Y	Y
1519	PanelCreate		Y	Y	Y	Y
1520	PanelOrder		Y	Y	Y	Y
1521	PanelTestAssign		Y	Y	Y	Y
1522	ElectronicOrderView		Y	Y	Y	Y
1523	ExternalConnectionsConfig		Y	Y	Y	Y
1524	ResultReportingConfiguration		Y	Y	Y	Y
\.


--
-- Data for Name: system_module_param; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_module_param (id, name, value) FROM stdin;
1	type	bacteriology
2	type	Biochemistry
3	type	chem
4	type	cytobacteriology
5	type	ECBU
6	type	EID
7	type	endocrin
8	type	hemato-immunology
9	type	Hematology
10	type	HIV
11	type	immuno
12	type	Immunology
13	type	liquidBio
14	type	Molecular Biology
15	type	mycobacteriology
16	type	mycology
17	type	mycrobacteriology
18	type	Parasitology
19	type	serologie
20	type	serology
21	type	Serology-Immunology
22	type	Virologie
23	type	VL
24	type	readonly
25	type	readwrite
26	type	initial
27	type	verify
28	type	indicator
29	type	patient
30	type	summary
31	type	Bacteria
32	type	Cytobacteriologie
33	type	Endocrinologie
34	type	Hemto-Immunology
35	type	Liquides biologique
36	type	VCT
37	type	virology
38	type	panel
39	type	test
40	provider	ConnectionTestProvider
\.


--
-- Name: system_module_param_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.system_module_param_seq', 40, true);


--
-- Name: system_module_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.system_module_seq', 1524, true);


--
-- Data for Name: system_module_url; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_module_url (id, url_path, system_module_id, system_module_param_id) FROM stdin;
1	/CancelDictionary	3	\N
2	/Dictionary	3	\N
3	/DictionaryMenu	3	\N
4	/SearchDictionaryMenu	3	\N
5	/DeleteDictionary	3	\N
6	/NextPreviousDictionary	3	\N
7	/CancelOrganization	6	\N
8	/Organization	6	\N
9	/NextPreviousOrganization	6	\N
10	/OrganizationMenu	6	\N
11	/SearchOrganizationMenu	6	\N
12	/DeleteOrganization	6	\N
13	/PatientResults	8	\N
14	/DeleteResultLimits	9	\N
15	/NextPreviousResultLimits	9	\N
16	/ResultLimits	9	\N
17	/ResultLimitsMenu	9	\N
18	/DeleteRole	11	\N
19	/NextPreviousRole	11	\N
20	/Role	11	\N
21	/RoleMenu	11	\N
22	/ManageInventory	18	\N
23	/StatusResults	26	\N
24	/DeleteUserRole	40	\N
25	/NextPreviousSystemUserOnePage	40	\N
26	/NextPreviousUserRole	40	\N
27	/UserRole	40	\N
28	/UserRoleMenu	40	\N
29	/PatientType	41	\N
30	/PatientTypeMenu	41	\N
31	/DeleteTypeOfSamplePanel	42	\N
32	/TypeOfSamplePanel	42	\N
33	/TypeOfSamplePanelMenu	42	\N
34	/DeleteTypeOfSampleTest	43	\N
35	/TypeOfSampleTest	43	\N
36	/TypeOfSampleTestMenu	43	\N
37	/DeleteUnifiedSystemUser	44	\N
38	/UnifiedSystemUser	44	\N
39	/UnifiedSystemUserMenu	44	\N
40	/LogbookResults	45	\N
41	/SamplePatientEntry	46	\N
42	/PatientManagement	46	\N
43	/SiteInformation	47	\N
44	/NextPreviousSiteInformation	47	\N
45	/CancelSiteInformation	47	\N
46	/DeleteSiteInformation	47	\N
47	/SiteInformationMenu	47	\N
48	/CancelAnalyzerTestName	48	\N
49	/AnalyzerTestName	48	\N
50	/AnalyzerTestNameMenu	48	\N
51	/DeleteAnalyzerTestName	48	\N
52	/AccessionResults	63	\N
53	/SampleEdit	106	\N
54	/NonConformity	107	\N
55	/ReferredOutTests	185	\N
56	/SampleConfirmationEntry	253	\N
57	/AuditTrailReport	1378	\N
58	/ResultValidation	1376	\N
59	/ResultValidationRetroC	1376	\N
60	/SampleEntryConfig	1393	\N
61	/NextPreviousSampleEntryConfig	1393	\N
62	/CancelSampleEntryConfig	1393	\N
63	/SampleEntryConfigMenu	1393	\N
64	/WorkplanConfiguration	1394	\N
65	/NextPreviousWorkplanConfiguration	1394	\N
66	/CancelWorkplanConfiguration	1394	\N
67	/DeleteWorkplanConfiguration	1394	\N
68	/WorkplanConfigurationMenu	1394	\N
69	/TestManagementConfigMenu	1395	\N
70	/TestRenameEntry	1396	\N
71	/PatientConfiguration	1397	\N
72	/NextPreviousPatientConfiguration	1397	\N
73	/CancelPatientConfiguration	1397	\N
74	/DeletePatientConfiguration	1397	\N
75	/PatientConfigurationMenu	1397	\N
76	/ResultConfiguration	1398	\N
77	/NextPreviousResultConfiguration	1398	\N
78	/CancelResultConfiguration	1398	\N
79	/DeleteResultConfiguration	1398	\N
80	/ResultConfigurationMenu	1398	\N
81	/PrintedReportsConfiguration	1399	\N
82	/NextPreviousPrintedReportsConfiguration	1399	\N
83	/CancelPrintedReportsConfiguration	1399	\N
84	/DeletePrintedReportsConfiguration	1399	\N
85	/PrintedReportsConfigurationMenu	1399	\N
86	/NonConformityConfiguration	1400	\N
87	/NextPreviousNonConformityConfiguration	1400	\N
88	/CancelNonConformityConfiguration	1400	\N
89	/DeleteNonConformityConfiguration	1400	\N
90	/NonConformityConfigurationMenu	1400	\N
91	/TestActivation	1401	\N
92	/ElisaAlgorithmWorkplan	1402	\N
93	/PrintWorkplanReport	1402	\N
94	/WorkPlanByTest	1402	\N
95	/WorkPlanByTestSection	1402	\N
96	/WorkPlanByPanel	1402	\N
97	/PanelRenameEntry	1404	\N
98	/TestOrderability	1405	\N
99	/TestCatalog	1406	\N
100	/TestSectionManagement	1408	\N
101	/TestSectionOrder	1409	\N
102	/TestSectionTestAssign	1410	\N
103	/TestSectionCreate	1411	\N
104	/SampleTypeRenameEntry	1412	\N
105	/MenuStatementConfig	1500	\N
106	/NextPreviousMenuStatementConfig	1500	\N
107	/CancelMenuStatementConfig	1500	\N
108	/DeleteMenuStatementConfig	1500	\N
109	/MenuStatementConfigMenu	1500	\N
110	/PrintBarcode	1508	\N
111	/TestSectionRenameEntry	1509	\N
112	/TestAdd	1511	\N
113	/PanelManagement	1512	\N
114	/SampleTypeManagement	1513	\N
115	/UomManagement	1514	\N
116	/UomCreate	1515	\N
117	/SampleTypeCreate	1516	\N
118	/SampleTypeOrder	1517	\N
119	/SampleTypeTestAssign	1518	\N
120	/PanelCreate	1519	\N
121	/PanelOrder	1520	\N
122	/PanelTestAssign	1521	\N
123	/ElectronicOrders	1522	\N
124	/ExternalConnectionsConfigMenu	1523	\N
125	/ResultReportingConfiguration	1524	\N
126	/SampleBatchEntryByProject	1505	\N
127	/SampleBatchEntry	1505	\N
128	/SampleBatchEntrySetup	1505	\N
129	/LogbookResults	19	1
130	/LogbookResults	1181	2
131	/LogbookResults	20	3
132	/LogbookResults	394	4
133	/LogbookResults	21	5
134	/LogbookResults	1421	6
135	/LogbookResults	397	7
136	/LogbookResults	1180	8
137	/LogbookResults	22	9
138	/LogbookResults	25	10
139	/LogbookResults	23	11
140	/LogbookResults	1179	12
141	/LogbookResults	396	13
142	/LogbookResults	395	14
143	/LogbookResults	322	15
144	/LogbookResults	645	16
145	/LogbookResults	393	17
146	/LogbookResults	24	18
147	/LogbookResults	657	19
148	/LogbookResults	60	20
149	/LogbookResults	1178	21
150	/LogbookResults	61	22
151	/LogbookResults	1424	23
152	/PatientEditByProject	117	24
153	/PatientEditByProject	116	25
154	/PatientEntryByProject	55	26
155	/PatientEntryByProject	56	27
156	/Report	110	28
157	/ReportPrint	110	28
158	/Report	108	29
159	/ReportPrint	108	29
160	/Report	109	30
161	/ReportPrint	109	30
162	/ResultValidation	1390	31
163	/ResultValidationRetroC	1390	31
164	/ResultValidation	114	2
165	/ResultValidationRetroC	114	2
166	/ResultValidation	1381	32
167	/ResultValidationRetroC	1381	32
168	/ResultValidation	1382	5
169	/ResultValidationRetroC	1382	5
170	/ResultValidation	1422	6
171	/ResultValidationRetroC	1422	6
172	/ResultValidation	1386	33
173	/ResultValidationRetroC	1386	33
174	/ResultValidation	113	9
175	/ResultValidationRetroC	113	9
176	/ResultValidation	1184	34
177	/ResultValidationRetroC	1184	34
178	/ResultValidation	112	12
179	/ResultValidationRetroC	112	12
180	/ResultValidation	1384	35
181	/ResultValidationRetroC	1384	35
182	/ResultValidation	1380	14
183	/ResultValidationRetroC	1380	14
184	/ResultValidation	1385	\N
185	/ResultValidationRetroC	1385	\N
186	/ResultValidation	1391	16
187	/ResultValidationRetroC	1391	16
188	/ResultValidation	1383	18
189	/ResultValidationRetroC	1383	18
190	/ResultValidation	1387	\N
191	/ResultValidationRetroC	1387	\N
192	/ResultValidation	111	20
193	/ResultValidationRetroC	111	20
194	/ResultValidation	1186	21
195	/ResultValidationRetroC	1186	21
196	/ResultValidation	1388	36
197	/ResultValidationRetroC	1388	36
198	/ResultValidation	1389	22
199	/ResultValidationRetroC	1389	22
200	/ResultValidation	115	37
201	/ResultValidationRetroC	115	37
202	/ResultValidation	1425	23
203	/ResultValidationRetroC	1425	23
204	/SampleEdit	123	24
205	/SampleEdit	118	25
206	/SampleEntryByProject	51	26
207	/SampleEntryEID	51	26
208	/SampleEntryVL	51	26
209	/SampleEntryByProject	52	27
210	/SampleEntryEID	52	27
211	/SampleEntryVL	52	27
212	/ElisaAlgorithmWorkplan	507	1
213	/PrintWorkplanReport	507	1
214	/WorkPlanByTest	507	1
215	/WorkPlanByTestSection	507	1
216	/WorkPlanByPanel	507	1
217	/ElisaAlgorithmWorkplan	73	2
218	/PrintWorkplanReport	73	2
219	/WorkPlanByTest	73	2
220	/WorkPlanByTestSection	73	2
221	/WorkPlanByPanel	73	2
222	/ElisaAlgorithmWorkplan	424	3
223	/PrintWorkplanReport	424	3
224	/WorkPlanByTest	424	3
225	/WorkPlanByTestSection	424	3
226	/WorkPlanByPanel	424	3
227	/ElisaAlgorithmWorkplan	425	4
228	/PrintWorkplanReport	425	4
229	/WorkPlanByTest	425	4
230	/WorkPlanByTestSection	425	4
231	/WorkPlanByPanel	425	4
232	/ElisaAlgorithmWorkplan	426	5
233	/PrintWorkplanReport	426	5
234	/WorkPlanByTest	426	5
235	/WorkPlanByTestSection	426	5
236	/WorkPlanByPanel	426	5
237	/ElisaAlgorithmWorkplan	1420	6
238	/PrintWorkplanReport	1420	6
239	/WorkPlanByTest	1420	6
240	/WorkPlanByTestSection	1420	6
241	/WorkPlanByPanel	1420	6
242	/ElisaAlgorithmWorkplan	433	7
243	/PrintWorkplanReport	433	7
244	/WorkPlanByTest	433	7
245	/WorkPlanByTestSection	433	7
246	/WorkPlanByPanel	433	7
247	/ElisaAlgorithmWorkplan	816	8
248	/PrintWorkplanReport	816	8
249	/WorkPlanByTest	816	8
250	/WorkPlanByTestSection	816	8
251	/WorkPlanByPanel	816	8
252	/ElisaAlgorithmWorkplan	72	9
253	/PrintWorkplanReport	72	9
254	/WorkPlanByTest	72	9
255	/WorkPlanByTestSection	72	9
256	/WorkPlanByPanel	72	9
257	/ElisaAlgorithmWorkplan	429	10
258	/PrintWorkplanReport	429	10
259	/WorkPlanByTest	429	10
260	/WorkPlanByTestSection	429	10
261	/WorkPlanByPanel	429	10
262	/ElisaAlgorithmWorkplan	428	11
263	/PrintWorkplanReport	428	11
264	/WorkPlanByTest	428	11
265	/WorkPlanByTestSection	428	11
266	/WorkPlanByPanel	428	11
267	/ElisaAlgorithmWorkplan	71	12
268	/PrintWorkplanReport	71	12
269	/WorkPlanByTest	71	12
270	/WorkPlanByTestSection	71	12
271	/WorkPlanByPanel	71	12
272	/ElisaAlgorithmWorkplan	431	13
273	/PrintWorkplanReport	431	13
274	/WorkPlanByTest	431	13
275	/WorkPlanByTestSection	431	13
276	/WorkPlanByPanel	431	13
277	/ElisaAlgorithmWorkplan	430	14
278	/PrintWorkplanReport	430	14
279	/WorkPlanByTest	430	14
280	/WorkPlanByTestSection	430	14
281	/WorkPlanByPanel	430	14
282	/ElisaAlgorithmWorkplan	644	16
283	/PrintWorkplanReport	644	16
284	/WorkPlanByTest	644	16
285	/WorkPlanByTestSection	644	16
286	/WorkPlanByPanel	644	16
287	/ElisaAlgorithmWorkplan	432	17
288	/PrintWorkplanReport	432	17
289	/WorkPlanByTest	432	17
290	/WorkPlanByTestSection	432	17
291	/WorkPlanByPanel	432	17
292	/ElisaAlgorithmWorkplan	561	38
293	/PrintWorkplanReport	561	38
294	/WorkPlanByTest	561	38
295	/WorkPlanByTestSection	561	38
296	/WorkPlanByPanel	561	38
297	/ElisaAlgorithmWorkplan	427	18
298	/PrintWorkplanReport	427	18
299	/WorkPlanByTest	427	18
300	/WorkPlanByTestSection	427	18
301	/WorkPlanByPanel	427	18
302	/ElisaAlgorithmWorkplan	434	19
303	/PrintWorkplanReport	434	19
304	/WorkPlanByTest	434	19
305	/WorkPlanByTestSection	434	19
306	/WorkPlanByPanel	434	19
307	/ElisaAlgorithmWorkplan	70	\N
308	/PrintWorkplanReport	70	\N
309	/WorkPlanByTest	70	\N
310	/WorkPlanByTestSection	70	\N
311	/WorkPlanByPanel	70	\N
312	/ElisaAlgorithmWorkplan	1183	21
313	/PrintWorkplanReport	1183	21
314	/WorkPlanByTest	1183	21
315	/WorkPlanByTestSection	1183	21
316	/WorkPlanByPanel	1183	21
317	/ElisaAlgorithmWorkplan	69	39
318	/PrintWorkplanReport	69	39
319	/WorkPlanByTest	69	39
320	/WorkPlanByTestSection	69	39
321	/WorkPlanByPanel	69	39
322	/ElisaAlgorithmWorkplan	74	22
323	/PrintWorkplanReport	74	22
324	/WorkPlanByTest	74	22
325	/WorkPlanByTestSection	74	22
326	/WorkPlanByPanel	74	22
327	/ElisaAlgorithmWorkplan	1423	23
328	/PrintWorkplanReport	1423	23
329	/WorkPlanByTest	1423	23
330	/WorkPlanByTestSection	1423	23
331	/WorkPlanByPanel	1423	23
332	/ajaxQueryXML	1524	40
\.


--
-- Name: system_module_url_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.system_module_url_seq', 332, true);


--
-- Data for Name: system_role; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_role (id, name, description, is_grouping_role, grouping_parent, display_key, active, editable) FROM stdin;
9	Results modifier              	Has permission to modify already entered results	f	\N	role.result.modifier	f	t
11	Audit Trail                   	Able to view the audit trail	f	\N	role.audittrail	t	f
12	Test Management               	Able to modify test catalog	f	\N	role.test.configuration	t	f
4	Intake                        	Sample entry and patient management.	f	\N	role.intake	t	f
6	Inventory mgr                 	Add and de/reactivate kits.	f	\N	role.inventory	f	f
1	Maintenance Admin             	Change tests, panels etc.	f	\N	role.maintenance	t	f
7	Reports                       	Generate reports.	f	\N	role.reports	t	f
5	Results entry                 	Enter and review results.	f	\N	role.results	t	f
2	User Admin                    	Add/remove users and assign roles.	f	\N	role.users	t	f
10	Validator                     	A person who can validate results	f	\N	role.validator	t	f
66	Results Admin                 	Able to access all results forms	f	\N	role.results.all	t	f
67	Identifying Information Edit  	Able to edit already entered patients and sample primary IDs	f	\N	role.edit.identifiers	t	f
21	Hematology automatic          	Able to transfer results from analyzer to OpenLIS	f	\N	role.hematology.automatic 	t	f
16	Immunology automatic          	Able to transfer results from analyzer to OpenLIS	f	\N	role.immuno.automatic 	t	f
26	Serology automatic            	Able to transfer results from analyzer to OpenLIS	f	\N	role.serology.automatic  	t	f
31	Biochemistry automatic        	Able to transfer results from analyzer to OpenLIS	f	\N	role.biochem.automatic  	t	f
36	Virology automatic            	Able to transfer results from analyzer to OpenLIS	f	\N	role.virology.automatic 	t	f
\.


--
-- Data for Name: system_role_module; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_role_module (id, has_select, has_add, has_update, has_delete, system_role_id, system_module_id) FROM stdin;
1	Y	Y	Y	Y	5	185
2	Y	Y	Y	Y	1	17
3	Y	Y	Y	Y	1	87
4	Y	Y	Y	Y	1	48
5	Y	Y	Y	Y	1	3
6	Y	Y	Y	Y	1	4
7	Y	Y	Y	Y	1	97
8	Y	Y	Y	Y	1	6
9	Y	Y	Y	Y	1	7
10	Y	Y	Y	Y	1	1
11	Y	Y	Y	Y	1	41
12	Y	Y	Y	Y	1	9
13	Y	Y	Y	Y	1	11
14	Y	Y	Y	Y	1	47
15	Y	Y	Y	Y	1	12
16	Y	Y	Y	Y	1	15
17	Y	Y	Y	Y	1	86
18	Y	Y	Y	Y	1	84
19	Y	Y	Y	Y	1	34
20	Y	Y	Y	Y	1	35
21	Y	Y	Y	Y	1	42
22	Y	Y	Y	Y	1	43
23	Y	Y	Y	Y	1	31
24	Y	Y	Y	Y	1	36
25	Y	Y	Y	Y	1	27
26	Y	Y	Y	Y	1	13
27	Y	Y	Y	Y	1	32
28	Y	Y	Y	Y	1	44
29	Y	Y	Y	Y	1	101
30	Y	Y	Y	Y	2	17
31	Y	Y	Y	Y	2	44
32	Y	Y	Y	Y	4	46
33	Y	Y	Y	Y	5	8
34	Y	Y	Y	Y	5	26
35	Y	Y	Y	Y	5	63
36	Y	Y	Y	Y	5	20
37	Y	Y	Y	Y	5	25
38	Y	Y	Y	Y	5	19
39	Y	Y	Y	Y	5	21
40	Y	Y	Y	Y	5	22
41	Y	Y	Y	Y	5	23
42	Y	Y	Y	Y	5	24
43	Y	Y	Y	Y	5	45
44	Y	Y	Y	Y	6	18
45	Y	Y	Y	Y	7	28
46	Y	Y	Y	Y	7	29
47	Y	Y	Y	Y	7	30
48	Y	Y	Y	Y	4	118
49	Y	Y	Y	Y	4	123
50	Y	Y	Y	Y	4	106
51	Y	Y	Y	Y	4	118
52	Y	Y	Y	Y	4	123
53	Y	Y	Y	Y	4	106
54	Y	Y	Y	Y	7	108
55	Y	Y	Y	Y	7	110
56	Y	Y	Y	Y	4	253
57	Y	Y	Y	Y	5	322
58	Y	Y	Y	Y	5	561
59	Y	Y	Y	Y	5	71
60	Y	Y	Y	Y	5	816
61	Y	Y	Y	Y	5	73
62	Y	Y	Y	Y	5	72
63	Y	Y	Y	Y	5	1183
64	Y	Y	Y	Y	5	1179
65	Y	Y	Y	Y	5	1180
66	Y	Y	Y	Y	5	1181
67	Y	Y	Y	Y	5	1178
68	Y	Y	Y	Y	10	112
69	Y	Y	Y	Y	10	1184
70	Y	Y	Y	Y	10	114
71	Y	Y	Y	Y	10	113
72	Y	Y	Y	Y	10	1186
73	Y	Y	Y	Y	5	69
74	Y	Y	Y	Y	4	107
75	Y	Y	Y	Y	5	107
76	Y	Y	Y	Y	11	1378
77	Y	Y	Y	Y	10	1380
78	Y	Y	Y	Y	5	395
79	Y	Y	Y	Y	5	430
80	Y	Y	Y	Y	1	1393
81	Y	Y	Y	Y	1	1394
82	Y	Y	Y	Y	10	1376
83	\N	\N	\N	\N	12	1395
84	\N	\N	\N	\N	12	1396
85	\N	\N	\N	\N	1	1397
86	\N	\N	\N	\N	1	1398
87	\N	\N	\N	\N	1	1399
88	\N	\N	\N	\N	1	1400
89	\N	\N	\N	\N	12	1401
90	Y	Y	Y	Y	5	1402
91	\N	\N	\N	\N	12	1404
92	\N	\N	\N	\N	12	1405
93	\N	\N	\N	\N	12	1406
94	Y	Y	Y	Y	12	17
95	Y	Y	Y	Y	12	1408
96	Y	Y	Y	Y	12	1409
97	Y	Y	Y	Y	12	1410
98	Y	Y	Y	Y	12	1411
99	Y	Y	Y	Y	12	1412
100	Y	Y	Y	Y	10	1387
101	Y	Y	Y	Y	5	60
102	Y	Y	Y	Y	5	70
103	Y	Y	Y	Y	1	1393
104	Y	Y	Y	Y	1	1394
105	Y	Y	Y	Y	1	1401
106	Y	Y	Y	Y	1	1404
107	Y	Y	Y	Y	1	1405
108	Y	Y	Y	Y	1	1406
109	Y	Y	Y	Y	1	1408
110	Y	Y	Y	Y	1	1409
111	Y	Y	Y	Y	1	1410
112	Y	Y	Y	Y	1	1411
113	Y	Y	Y	Y	1	1412
114	Y	Y	Y	Y	4	51
115	Y	Y	Y	Y	4	52
116	Y	Y	Y	Y	4	53
117	Y	Y	Y	Y	4	54
118	Y	Y	Y	Y	4	55
119	Y	Y	Y	Y	4	56
120	Y	Y	Y	Y	4	116
121	Y	Y	Y	Y	4	173
122	Y	Y	Y	Y	4	174
123	Y	Y	Y	Y	1	1500
124	Y	Y	Y	Y	5	70
125	Y	Y	Y	Y	5	1501
126	Y	Y	Y	Y	10	1502
127	Y	Y	Y	Y	10	1503
128	Y	Y	Y	Y	10	1504
129	Y	Y	Y	Y	4	117
130	Y	Y	Y	Y	5	74
131	Y	Y	Y	Y	5	61
132	Y	Y	Y	Y	10	1389
133	Y	Y	Y	Y	67	1100
134	Y	Y	Y	Y	67	1101
135	Y	Y	Y	Y	5	1420
136	Y	Y	Y	Y	5	1421
137	Y	Y	Y	Y	5	1422
138	Y	Y	Y	Y	5	1423
139	Y	Y	Y	Y	5	1424
140	Y	Y	Y	Y	5	1425
141	Y	Y	Y	Y	5	427
142	Y	Y	Y	Y	10	1383
143	Y	Y	Y	Y	4	1505
144	Y	Y	Y	Y	4	1506
145	Y	Y	Y	Y	4	1507
146	Y	Y	Y	Y	4	1508
147	Y	Y	Y	Y	12	1509
148	Y	Y	Y	Y	12	1510
149	Y	Y	Y	Y	12	1511
150	Y	Y	Y	Y	12	1513
151	Y	Y	Y	Y	12	1512
152	Y	Y	Y	Y	12	1514
153	Y	Y	Y	Y	12	1515
154	Y	Y	Y	Y	12	1516
155	Y	Y	Y	Y	12	1517
156	Y	Y	Y	Y	12	1518
157	Y	Y	Y	Y	12	1519
158	Y	Y	Y	Y	12	1520
159	Y	Y	Y	Y	12	1521
160	Y	Y	Y	Y	67	1522
161	Y	Y	Y	Y	11	1522
162	Y	Y	Y	Y	4	1522
163	Y	Y	Y	Y	1	1522
164	Y	Y	Y	Y	5	1522
165	Y	Y	Y	Y	10	1522
166	Y	Y	Y	Y	66	1522
167	Y	Y	Y	Y	1	1523
168	Y	Y	Y	Y	1	1524
\.


--
-- Name: system_role_module_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.system_role_module_seq', 168, true);


--
-- Name: system_role_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.system_role_seq', 70, false);


--
-- Data for Name: system_user; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_user (id, external_id, login_name, last_name, first_name, initials, is_active, is_employee, lastupdated) FROM stdin;
1	1	admin	ELIS	Open	OE	Y	Y	2006-11-08 11:11:14.312
109	1	serviceUser	External	Service	\N	N	N	2013-04-17 10:44:01.436961
\.


--
-- Data for Name: system_user_module; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_user_module (id, has_select, has_add, has_update, has_delete, system_user_id, system_module_id) FROM stdin;
\.


--
-- Name: system_user_module_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.system_user_module_seq', 461, true);


--
-- Data for Name: system_user_role; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_user_role (system_user_id, role_id) FROM stdin;
\.


--
-- Data for Name: system_user_section; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.system_user_section (id, has_view, has_assign, has_complete, has_release, has_cancel, system_user_id, test_section_id) FROM stdin;
\.


--
-- Name: system_user_section_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.system_user_section_seq', 1, false);


--
-- Name: system_user_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.system_user_seq', 112, true);


--
-- Data for Name: test; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test (id, method_id, uom_id, description, loinc, reporting_description, sticker_req_flag, is_active, active_begin, active_end, is_reportable, time_holding, time_wait, time_ta_average, time_ta_warning, time_ta_max, label_qty, lastupdated, label_id, test_trailer_id, test_section_id, scriptlet_id, test_format_id, local_code, sort_order, name, orderable, guid, name_localization_id, reporting_name_localization_id) FROM stdin;
1	\N	29	Transaminases GPT (37°C)(Serum)	\N	TGP	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	GPT/ALAT-Serum	10	Transaminases GPT (37°C)	t	3a3661a1-a166-4590-90bc-937912789739	3	4
2	\N	29	Transaminases G0T (37°C)(Serum)	\N	TGO	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	GOT/ASAT-Serum	20	Transaminases G0T (37°C)	t	71a02f4a-70b8-47da-9527-b6d604a92921	5	6
3	\N	25	Glucose(Plasma)	\N	Glycémie	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Glucose-Plasma	30	Glucose	t	8410a83b-d09a-475d-a71c-1fcbcca94e58	7	8
4	\N	27	Créatinine(Serum)	\N	Creatinine	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Creatinine-Serum	40	Créatinine	t	d7f672c4-52ea-4c26-bdf0-e9527d2ba95f	9	10
5	\N	29	Amylase(Serum)	\N	Amylase	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Amylase-Serum	50	Amylase	t	b45cace4-5436-41c3-a4df-bcc9c11395eb	11	12
6	\N	25	Albumine recherche miction(Urines)	\N	Albumine	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Albumin-Urines	60	Albumine recherche miction	t	3578f53d-8e5c-4e16-9dd3-65fe4d1a1f4d	13	14
57	\N	\N	Determine(Sérum)		Determine-Sérum	\N	\N	\N	\N	N	\N	\N	\N	\N	\N	\N	2013-10-10 16:52:59.678901	\N	\N	117	\N	\N	Murex-Serum	480	Determine	t	585a6c2a-93ca-454f-ace3-f9a5fa61d33d	115	116
58	\N	\N	Determine(Whole Blood)		Determine-Sang total	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2013-10-10 16:52:59.678901	\N	\N	117	\N	\N	Vironostika-Blood	490	Determine	t	1f6ebdb4-0305-4fe6-8112-4b8cc8644298	117	118
7	\N	25	Cholestérol total(Serum)	\N	Chol total	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Total cholesterol-Serum	70	Cholestérol total	t	4cd86c73-eca4-4968-a410-ee9d61f5da11	15	16
8	\N	25	Cholestérol HDL(Serum)	\N	Chol HDL	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	HDL cholesterol-Serum	80	Cholestérol HDL	t	eea822b4-5535-4e25-b9ef-fd492bef4349	17	18
9	\N	25	Triglycérides(Serum)	\N	Triglycérides	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Triglicerides-Serum	90	Triglycérides	t	59321bd7-ab24-43a2-b47c-557f283548ff	19	20
10	\N	25	Prolans (BHCG) urines de 24 h(Urines)	\N	Béta HCG – 24 h	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Beta HCG-Urines	100	Prolans (BHCG) urines de 24 h	t	fd50b89e-0f16-485f-a599-8ed989efa855	21	22
11	\N	\N	Test urinaire de grossesse(Urine)	\N	Test Grossesse	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Urine prenancy test-Urine	110	Test urinaire de grossesse	t	18ee4ac6-9a1c-4bfa-ad8a-08968a412785	23	24
12	\N	\N	Protéinurie sur bandelette(Urine)	\N	Protéines	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	56	\N	\N	Proteinuria dipstick-Urine	120	Protéinurie sur bandelette	t	a44239e6-bd4d-4ffa-98eb-549b5102207a	25	26
13	\N	31	Numération des globules blancs(Whole Blood)	\N	GB	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	White Blood Cells Count (WBC)-Blood	130	Numération des globules blancs	t	e08bdd35-b7e4-4910-ae73-da5b6447e901	27	28
14	\N	36	Numération des globules rouges(Whole Blood)	\N	GR	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Red Blood Cells Count (RBC)-Blood	140	Numération des globules rouges	t	25249ec2-0dbb-4a45-8c97-836c175ab183	29	30
15	\N	12	Hémoglobine(Whole Blood)	\N	Hb	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Hemoglobin-Blood	150	Hémoglobine	t	466b3775-e117-4268-92a7-3d3de95d43b3	31	32
16	\N	5	Hémotocrite(Whole Blood)	\N	Hte	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Hematocrit-Blood	160	Hémotocrite	t	4dea29d7-09aa-4ae4-92e9-aed3cde44462	33	34
17	\N	32	Volume Globulaire Moyen(Whole Blood)	\N	VGM	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Medium corpuscular volum-Blood	170	Volume Globulaire Moyen	t	8980331d-7d69-4364-a793-e1855ea58360	35	36
184	\N	5	HCT	\N	HCT	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	HCT	180	HCT	t	6792a51e-050b-4493-88ca-6f490c20cc5c	248	249
186	\N	41	GR	\N	GR	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	GR	160	GR	t	fe6405c8-f96b-491b-95c9-b1f635339d6a	250	251
187	\N	12	Hb	\N	Hb	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	Hb	170	Hb	t	cecea358-1fa0-44b2-8185-d8c010315f78	252	253
188	\N	32	VGM	\N	VGM	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	VGM	190	VGM	t	ddce6c12-e319-455f-9f48-2f6ff363a246	254	255
189	\N	16	TCMH	\N	TCMH	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	TCMH	200	TCMH	t	bf497153-ba88-4fe8-83ee-c144229d7628	256	257
185	\N	37	CCMH	\N	CCMH	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	CCMH	210	CCMH	t	8ab87a81-6b6b-4d4b-b53b-fac57109e393	258	259
190	\N	5	CD3 percentage count	\N	CD3 percentage count	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	59	\N	\N	CD3 percentage count	270	CD3 percentage count	t	ab362fc9-56bb-4f65-93d4-aed5bd8e3c8c	260	261
191	\N	5	CD4 percentage count	\N	CD4 percentage count	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	59	\N	\N	CD4 percentage count	280	CD4 percentage count	t	cb6029e2-ff76-4df0-812d-88539cccba28	262	263
192	\N	45	CD4 absolute count	\N	CD4 absolute count	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	59	\N	\N	CD4 absolute count	290	CD4 absolute count	t	a6718123-8d56-4103-9bbe-26b19306b83d	264	265
193	\N	\N	NE#	\N	NE#	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	NE#	0	NE#	t	53736980-edf1-4abf-aa6b-7ab2dbf3e7c4	266	267
194	\N	\N	MO#	\N	MO#	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	MO#	0	MO#	t	a11305b0-5c64-4661-941b-de4c3ef5e61e	268	269
195	\N	\N	BA#	\N	BA#	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	BA#	0	BA#	t	ee97e167-6269-4095-99a1-19f5c75bb94b	270	271
196	\N	\N	LY#	\N	LY#	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	LY#	0	LY#	t	496e3a90-968a-4c54-a101-72fb43d0b4ee	272	273
197	\N	\N	EO#	\N	EO#	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	EO#	0	EO#	t	cd73ec28-f5d8-47d6-b079-e2ba8f506f8f	274	275
198	\N	\N	Bioline	\N	Bioline	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Bioline	45	Bioline	t	c98ef346-76e1-4d16-9964-71cca4396de5	276	277
199	\N	\N	Innolia	\N	Innolia	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Innolia	40	Innolia	t	de47583b-3dcc-46bf-bfa2-f03d8bc8670d	278	279
18	\N	16	Teneur Corpusculaire Moyenne en Hémoglobine(Whole Blood)	\N	TCMH	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	TMCH-Blood	180	Teneur Corpusculaire Moyenne en Hémoglobine	t	78e49ba2-72f8-49df-99f1-5fc2a3c0914c	37	38
19	\N	37	Concentration Corpusculaire Moyenne en Hémoglobine(Whole Blood)	\N	CCMH	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	CMCH-Blood	190	Concentration Corpusculaire Moyenne en Hémoglobine	t	a7d20177-a559-4829-ad77-94b9b1ff025b	39	40
20	\N	31	Plaquette(Whole Blood)	\N	Plaquettes	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Platelets-Blood	200	Plaquette	t	17ff4ca7-b8b6-44a1-bae0-97f38affc35c	41	42
21	\N	5	Polynucléaires Neutrophiles (%)(Whole Blood)	\N	PNN %	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Neutrophiles (%)-Blood	210	Polynucléaires Neutrophiles (%)	t	c8979eb1-f975-4b77-9963-1328e95c5338	43	44
22	\N	33	Polynucléaires Neutrophiles (Abs)(Whole Blood)	\N	PNN (Abs)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Neutrophiles-Blood	220	Polynucléaires Neutrophiles (Abs)	t	5bf892d4-4b5d-4f7f-9fea-d54c8d9631df	45	46
23	\N	5	Polynucléaires Eosinophiles (%)(Whole Blood)	\N	PNE (%)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Eosinophiles (%)-Blood	230	Polynucléaires Eosinophiles (%)	t	0cdc2eed-19ea-4c36-beed-662273852506	47	48
24	\N	33	Polynucléaires Eosinophiles (Abs)(Whole Blood)	\N	PNE (Abs)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Eosinophiles-Blood	240	Polynucléaires Eosinophiles (Abs)	t	f0bbc211-66d2-4219-a377-79a9869a8413	49	50
25	\N	5	Polynucléaires basophiles (%)(Whole Blood)	\N	PNB (%)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Basophiles (%)-Blood	250	Polynucléaires basophiles (%)	t	e79dba96-ce3e-4b3c-945b-a73f7fa4b862	51	52
26	\N	33	Polynucléaires basophiles (Abs)(Whole Blood)	\N	PNB (Abs)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Basophiles-Blood	260	Polynucléaires basophiles (Abs)	t	febdc29b-78ac-48f8-afba-b7da2a1fb3c2	53	54
27	\N	5	Lymphocytes (%)(Whole Blood)	\N	Lympho (%)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Lymphocytes (%)-Blood	270	Lymphocytes (%)	t	7a4f53a3-b1ab-457b-b928-6c69f30aeb27	55	56
28	\N	33	Lymphocytes (Abs)(Whole Blood)	\N	Lympho (Abs)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Lymphocytes (Abs)-Blood	280	Lymphocytes (Abs)	t	e182fd13-38e6-4c5e-9ea7-f3635a957a78	57	58
29	\N	5	Monocytes (%)(Whole Blood)	\N	Mono (%)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Monocytes (%)-Blood	290	Monocytes (%)	t	79391ea9-9a96-484c-b8d7-c261a5cfffc0	59	60
30	\N	33	Monocytes (Abs)(Whole Blood)	\N	Mono (Abs)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	36	\N	\N	Monocytes (Abs)-Blood	300	Monocytes (Abs)	t	545d87fd-7959-4d53-bf9a-c87a7d2af680	61	62
31	\N	\N	Test rapide HIV(Serum)	\N	VIH rapide-Serum	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	HIV rapid test HIV-Serum	310	Test rapide HIV	t	c200173b-d972-4e54-9c4f-5271290a8ed8	63	64
32	\N	\N	Test rapide HIV(Plasma)	\N	VIH rapide-Plasm	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	HIV rapid test HIV-Plasma	320	Test rapide HIV	t	d0ec0286-44cd-485d-ac0c-87d3664198a6	65	66
33	\N	\N	Test rapide HIV(Whole Blood)	\N	VIH rapide-SangT	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	HIV rapid test HIV-Blood	330	Test rapide HIV	t	0ac0b77e-672c-4eee-ae71-c05a0fee086b	67	68
34	\N	7	Dénombrement des lymphocytes CD4 (mm3)(Whole Blood)	\N	CD4  (Abs)	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	59	\N	\N	CD4 Absolute count (mm3)-Blood	340	Dénombrement des lymphocytes CD4 (mm3)	t	1d329af4-e1af-43c8-a533-5000bfdd868a	69	70
159	\N	25	Glycémie	\N	Glycémie	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	56	\N	\N	Glycémie	260	Glycémie	t	3cf20999-c453-4246-b5d4-a39060101b79	200	201
160	\N	27	Créatininémie	\N	Créatininémie	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	56	\N	\N	Créatininémie	250	Créatininémie	t	db25d5fe-4a7a-4e94-8dec-d57b8c13108a	202	203
161	\N	26	Transaminases ALTL	\N	Transaminases ALTL	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	56	\N	\N	Transaminases ALTL	230	Transaminases ALTL	t	5f1c1a36-5147-45a8-8c7b-162ff43e5145	204	205
162	\N	\N	p24 Ag	\N	p24 Ag	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	p24 Ag	90	p24 Ag	t	d215dd9d-16aa-4c1f-8289-94c4c7ed040b	206	207
163	\N	\N	Western Blot 2	\N	Western Blot 2	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Western Blot 2	80	Western Blot 2	t	74b2c59e-1ae0-4a3d-8451-6103b341c85c	208	209
164	\N	\N	Western Blot 1	\N	Western Blot 1	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Western Blot 1	70	Western Blot 1	t	a1eac50b-2ef8-4e90-b1a8-9748cf94af7d	210	211
165	\N	\N	Genie II 10	\N	Genie II 10	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Genie II 10	60	Genie II 10	t	d7aac329-ba07-4dfe-b8eb-9b8f679115fb	212	213
166	\N	\N	Genie II 100	\N	Genie II 100	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Genie II 100	50	Genie II 100	t	cd8b425e-b504-421a-a053-a0b62704c68d	214	215
167	\N	\N	Genie II	\N	Genie II	\N	N	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Genie II	40	Genie II	t	0ae4006c-3aa9-41f2-9659-c3289c394e2b	216	217
168	\N	\N	Vironostika	\N	Vironostika	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Vironostika	30	Vironostika	t	2b97c89d-3f97-495c-90e1-d1c5c6a4d8fa	218	219
169	\N	\N	Murex	\N	Murex	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Murex	10	Murex	t	4b758755-a41e-45d3-8558-73a532253b71	220	221
170	\N	\N	Integral	\N	Integral	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	97	\N	\N	Integral	20	Integral	t	61bc9b10-8d97-46bb-8e04-04eb1b990433	222	223
172	\N	\N	CD4	\N	CD4	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	59	\N	\N	CD4	0	CD4	t	acfe5351-f531-4011-acd1-7c473b3f5da3	224	225
173	\N	\N	Génotypage	\N	Génotypage	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	76	\N	\N	Génotypage	310	Génotypage	t	9df504a9-297c-4137-9928-4ad8101cd690	226	227
174	\N	30	Viral Load	\N	Viral Load	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	76	\N	\N	Viral Load	320	Viral Load	t	b50d156e-0f6f-40cd-921c-4e831602a623	228	229
175	\N	\N	DNA PCR	\N	DNA PCR	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	76	\N	\N	DNA PCR	300	DNA PCR	t	c1afd23c-c30f-42d7-af48-4321c069f48f	230	231
176	\N	26	Transaminases ASTL	\N	Transaminases ASTL	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	56	\N	\N	Transaminases ASTL	240	Transaminases ASTL	t	1d094fe0-7fc7-42eb-ba54-15b24ba44ae9	232	233
177	\N	39	GB	\N	GB	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	GB	100	GB	t	0e240569-c095-41c7-bfd2-049527452f16	234	235
178	\N	39	PLQ	\N	PLQ	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	PLQ	220	PLQ	t	88b7d8d3-e82b-441f-aff3-1410ba2850a5	236	237
179	\N	5	Neut %	\N	Neut %	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	Neut %	110	Neut %	t	0c25692f-a321-4e9c-9722-ca73f6625cb9	238	239
180	\N	5	Lymph %	\N	Lymph %	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	Lymph %	120	Lymph %	t	eede92e7-d141-4c76-ab6e-b24ccfc84215	240	241
35	\N	5	Dénombrement des lymphocytes  CD4 (%)(Whole Blood)	\N	CD4 %	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	59	\N	\N	CD4 percent  (%)-Blood	350	Dénombrement des lymphocytes  CD4 (%)	t	614652de-5e04-4fe7-a897-77d976317d2b	71	72
36	\N	\N	HBs AG (antigén australia)(Serum)	\N	Ag HBs	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	HBsAg (Hepatitis B surface antigen)-Serum	360	HBs AG (antigén australia)	t	bc3ab337-3287-477b-9f52-cb0d0db4f06a	73	74
37	\N	30	Mesure de la charge virale(Whole Blood)	\N	Charge virale VIH	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	136	\N	\N	Viral load-Blood	370	Mesure de la charge virale	t	5c37ba62-1e04-46ab-8db6-82db9c6fbb5e	75	76
38	\N	30	Detection de la resistance aux antiretroviraux(Whole Blood)	\N	ARV res	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	136	\N	\N	ARV resistance-Blood	380	Detection de la resistance aux antiretroviraux	t	d83c247c-ccf4-4b9a-9016-357085672fac	77	78
39	\N	\N	Western blot VIH(Serum)	\N	WB-Serum	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Western blot HIV-Serum	390	Western blot VIH	t	6e654c26-0a55-4867-a168-e55e6516fd1e	79	80
40	\N	\N	Western blot VIH(Plasma)	\N	WB-Plasma	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Western blot HIV-Plasma	400	Western blot VIH	t	ace3f934-4b94-4fbe-8a82-1f4cec317347	81	82
41	\N	\N	Bioline(Plasma)	\N	Bioline-Plasma	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Bioline-Plasma	410	Bioline	t	9b43af6e-25e4-4ebf-9724-d7bdf71a62c5	83	84
42	\N	\N	Bioline(Serum)	\N	Bioline-Serum	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Bioline-Serum	420	Bioline	t	2e225bfc-e9a3-4ce2-aefb-16364cb2df3b	85	86
43	\N	\N	Bioline(Whole Blood)	\N	Bioline-Sang total	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Bioline-Blood	430	Bioline	t	d7384982-3646-409e-a37d-019da248623f	87	88
44	\N	\N	Genie III(Plasma)	\N	Genie III-Plasma	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Genie III-Plasma	440	Genie III	f	8a529b1a-72e6-4562-96ac-885b758f3280	89	90
45	\N	\N	Genie III(Serum)	\N	Genie III-Serum	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Genie III-Serum	450	Genie III	f	098def0a-779e-496a-afd2-b19ca94c4c94	91	92
46	\N	\N	Genie III(Whole Blood)	\N	Genie III-Sang t	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Genie III-Blood	460	Genie III	f	bea00384-ce9f-44ec-aca1-81c58edadb43	93	94
47	\N	\N	Murex(Plasma)	\N	Murex-Plasma	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Murex-Plasma	470	Murex	t	74bb0da6-c7fe-44f8-810c-a72d38fd8fc7	95	96
48	\N	\N	Murex(Serum)	\N	Murex-Serum	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Murex-Serum	480	Murex	t	2587b1f1-4b82-4d6f-a64e-e4fcc2230e1c	97	98
49	\N	\N	Vironostika(Plasma)	\N	Vironostika-Plas	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Vironostika-Plasma	490	Vironostika	t	01f90ef2-6afe-4928-8249-2e64e4c02e88	99	100
50	\N	\N	Vironostika(Serum)	\N	Vironostika-Seru	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	Vironostika-Serum	500	Vironostika	t	704c585b-8e95-4550-9b11-573ab1c2cfb5	101	102
51	\N	\N	P24 Ag(Plasma)	\N	P24 Ag-Plasma	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	P24 Ag-Plasma	510	P24 Ag	t	ac8003af-6187-4ea0-8410-c320fb9d7dda	103	104
52	\N	\N	P24 Ag(Serum)	\N	P24 Ag-Serum	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2012-10-24 11:58:25.267898	\N	\N	117	\N	\N	P24 Ag-Serum	520	P24 Ag	t	103fc942-99f1-4991-858b-bd66aa9c3374	105	106
53	\N	\N	Stat-Pak(Plasma)		Stat-Pak-Plasma	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2013-10-10 16:52:59.678901	\N	\N	117	\N	\N	Stat-Pak-Plasma	434	Stat-Pak	f	05ab0ed9-b9cf-4d9b-9362-8c4f4ebbd614	107	108
54	\N	\N	Stat-Pak(Sérum)		Stat-Pak-Sérum	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2013-10-10 16:52:59.678901	\N	\N	117	\N	\N	Stat-Pak-Serum	435	Stat-Pak	f	df43c9a3-adc5-4f39-946c-0fdc63692a8d	109	110
55	\N	\N	Stat-Pak(Whole Blood)		Stat-Pak-Sang total	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2013-10-10 16:52:59.678901	\N	\N	117	\N	\N	Stat-Pak-Blood	436	Stat-Pak	f	d7c06cb7-f038-4a73-a432-7ca9e25062c5	111	112
56	\N	\N	Determine(Plasma)		Determine-Plasma	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2013-10-10 16:52:59.678901	\N	\N	117	\N	\N	Determine-Plasma	470	Determine	t	679972ca-dce3-4e2d-bd13-8c70b24c299e	113	114
181	\N	5	Mono %	\N	Mono %	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	Mono %	130	Mono %	t	9eece97f-04f3-4381-b378-2a9ac08a535a	242	243
182	\N	5	Eo %	\N	Eo %	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	Eo %	140	Eo %	t	50b568e8-e9da-428d-9697-8080bca7377b	244	245
183	\N	5	Baso %	\N	Baso %	\N	Y	\N	\N	N	\N	\N	\N	\N	\N	\N	2020-01-22 21:46:41.948131	\N	\N	36	\N	\N	Baso %	150	Baso %	t	a41fcfb4-e3ba-4add-ac5d-56fae322cb9e	246	247
\.


--
-- Data for Name: test_analyte; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_analyte (id, test_id, analyte_id, result_group, sort_order, testalyt_type, lastupdated, is_reportable) FROM stdin;
1	56	98	10	1	\N	2013-10-10 16:53:00.019523	\N
2	57	98	10	1	\N	2013-10-10 16:53:00.019523	\N
3	58	98	10	1	\N	2013-10-10 16:53:00.019523	\N
4	44	99	10	1	\N	2013-10-10 16:53:00.019523	\N
5	45	99	10	1	\N	2013-10-10 16:53:00.019523	\N
6	46	99	10	1	\N	2013-10-10 16:53:00.019523	\N
278	162	111	9	1	\N	2020-01-22 21:46:41.995858	\N
279	163	112	8	1	\N	2020-01-22 21:46:41.995858	\N
280	164	113	7	1	\N	2020-01-22 21:46:41.995858	\N
281	165	114	6	1	\N	2020-01-22 21:46:41.995858	\N
282	166	115	5	1	\N	2020-01-22 21:46:41.995858	\N
283	167	116	4	1	\N	2020-01-22 21:46:41.995858	\N
285	169	101	2	1	\N	2020-01-22 21:46:41.995858	\N
286	170	102	1	1	\N	2020-01-22 21:46:41.995858	\N
287	191	103	11	1	\N	2020-01-22 21:46:41.995858	\N
288	177	104	12	1	\N	2020-01-22 21:46:41.995858	\N
289	180	105	13	1	\N	2020-01-22 21:46:41.995858	\N
290	198	108	14	1	\N	2020-01-22 21:46:41.995858	\N
291	199	109	14	1	\N	2020-01-22 21:46:41.995858	\N
284	168	100	3	2	\N	2020-01-22 21:46:41.995858	\N
292	168	110	15	1	\N	2020-01-22 21:46:41.995858	\N
\.


--
-- Name: test_analyte_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.test_analyte_seq', 300, false);


--
-- Data for Name: test_code; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_code (test_id, code_type_id, value, lastupdated) FROM stdin;
1	3	B 20	2012-10-24 11:58:25.663941-07
1	4	12214	2012-10-24 11:58:25.663941-07
2	3	B 20	2012-10-24 11:58:25.663941-07
2	4	12213	2012-10-24 11:58:25.663941-07
3	3	B 10	2012-10-24 11:58:25.663941-07
3	4	11918	2012-10-24 11:58:25.663941-07
4	3	B 20	2012-10-24 11:58:25.663941-07
4	4	11914	2012-10-24 11:58:25.663941-07
5	3	B 50	2012-10-24 11:58:25.663941-07
5	4	12201	2012-10-24 11:58:25.663941-07
6	3	B 10	2012-10-24 11:58:25.663941-07
6	4	11519	2012-10-24 11:58:25.663941-07
7	3	B 15	2012-10-24 11:58:25.663941-07
7	4	11911	2012-10-24 11:58:25.663941-07
8	3	B 15	2012-10-24 11:58:25.663941-07
8	4	11912	2012-10-24 11:58:25.663941-07
9	3	B 30	2012-10-24 11:58:25.663941-07
9	4	11929	2012-10-24 11:58:25.663941-07
10	3	B150	2012-10-24 11:58:25.663941-07
10	4		2012-10-24 11:58:25.663941-07
11	3	B 150	2012-10-24 11:58:25.663941-07
11	4		2012-10-24 11:58:25.663941-07
12	3	B 7	2012-10-24 11:58:25.663941-07
12	4		2012-10-24 11:58:25.663941-07
13	3	B30	2012-10-24 11:58:25.663941-07
13	4	11116	2012-10-24 11:58:25.663941-07
14	3	B30	2012-10-24 11:58:25.663941-07
14	4	11116	2012-10-24 11:58:25.663941-07
15	3	B30	2012-10-24 11:58:25.663941-07
15	4	11116	2012-10-24 11:58:25.663941-07
16	3	B30	2012-10-24 11:58:25.663941-07
16	4	11116	2012-10-24 11:58:25.663941-07
17	3	B30	2012-10-24 11:58:25.663941-07
17	4	11116	2012-10-24 11:58:25.663941-07
18	3	B30	2012-10-24 11:58:25.663941-07
18	4	11116	2012-10-24 11:58:25.663941-07
19	3	B30	2012-10-24 11:58:25.663941-07
19	4	11116	2012-10-24 11:58:25.663941-07
20	3	B30	2012-10-24 11:58:25.663941-07
20	4	11116	2012-10-24 11:58:25.663941-07
21	3	B30	2012-10-24 11:58:25.663941-07
21	4	11116	2012-10-24 11:58:25.663941-07
22	3	B30	2012-10-24 11:58:25.663941-07
22	4	11116	2012-10-24 11:58:25.663941-07
23	3	B30	2012-10-24 11:58:25.663941-07
23	4	11116	2012-10-24 11:58:25.663941-07
24	3	B30	2012-10-24 11:58:25.663941-07
24	4	11116	2012-10-24 11:58:25.663941-07
25	3	B30	2012-10-24 11:58:25.663941-07
25	4	11116	2012-10-24 11:58:25.663941-07
26	3	B30	2012-10-24 11:58:25.663941-07
26	4	11116	2012-10-24 11:58:25.663941-07
27	3	B30	2012-10-24 11:58:25.663941-07
27	4	11116	2012-10-24 11:58:25.663941-07
28	3	B30	2012-10-24 11:58:25.663941-07
28	4	11116	2012-10-24 11:58:25.663941-07
29	3	B30	2012-10-24 11:58:25.663941-07
29	4	11116	2012-10-24 11:58:25.663941-07
30	3	B30	2012-10-24 11:58:25.663941-07
30	4	11116	2012-10-24 11:58:25.663941-07
31	3	B 30	2012-10-24 11:58:25.663941-07
31	4		2012-10-24 11:58:25.663941-07
32	3	B 30	2012-10-24 11:58:25.663941-07
32	4		2012-10-24 11:58:25.663941-07
33	3	B 30	2012-10-24 11:58:25.663941-07
33	4		2012-10-24 11:58:25.663941-07
34	3	B100	2012-10-24 11:58:25.663941-07
34	4	12802	2012-10-24 11:58:25.663941-07
35	3	B100	2012-10-24 11:58:25.663941-07
35	4		2012-10-24 11:58:25.663941-07
36	3	B 60	2012-10-24 11:58:25.663941-07
36	4	11202	2012-10-24 11:58:25.663941-07
37	3	B 150	2012-10-24 11:58:25.663941-07
37	4		2012-10-24 11:58:25.663941-07
38	3	B 150	2012-10-24 11:58:25.663941-07
38	4		2012-10-24 11:58:25.663941-07
39	3	B 150	2012-10-24 11:58:25.663941-07
39	4		2012-10-24 11:58:25.663941-07
40	3	B 150	2012-10-24 11:58:25.663941-07
40	4		2012-10-24 11:58:25.663941-07
41	3	B 65	2012-10-24 11:58:25.663941-07
41	4		2012-10-24 11:58:25.663941-07
42	3	B 65	2012-10-24 11:58:25.663941-07
42	4		2012-10-24 11:58:25.663941-07
43	3	B 65	2012-10-24 11:58:25.663941-07
43	4		2012-10-24 11:58:25.663941-07
44	3	B 65	2012-10-24 11:58:25.663941-07
44	4		2012-10-24 11:58:25.663941-07
45	3	B 65	2012-10-24 11:58:25.663941-07
45	4		2012-10-24 11:58:25.663941-07
46	3	B 65	2012-10-24 11:58:25.663941-07
46	4		2012-10-24 11:58:25.663941-07
47	3	B 65	2012-10-24 11:58:25.663941-07
47	4		2012-10-24 11:58:25.663941-07
48	3	B 65	2012-10-24 11:58:25.663941-07
48	4		2012-10-24 11:58:25.663941-07
49	3	B 65	2012-10-24 11:58:25.663941-07
49	4		2012-10-24 11:58:25.663941-07
50	3	B 65	2012-10-24 11:58:25.663941-07
50	4		2012-10-24 11:58:25.663941-07
51	3	B 65	2012-10-24 11:58:25.663941-07
51	4		2012-10-24 11:58:25.663941-07
52	3	B 65	2012-10-24 11:58:25.663941-07
52	4		2012-10-24 11:58:25.663941-07
53	3	B 65	2013-10-10 16:52:59.962614-07
54	3	B 65	2013-10-10 16:52:59.962614-07
55	3	B 65	2013-10-10 16:52:59.962614-07
56	3	B 65	2013-10-10 16:52:59.962614-07
57	3	B 65	2013-10-10 16:52:59.962614-07
58	3	B 65	2013-10-10 16:52:59.962614-07
\.


--
-- Data for Name: test_code_type; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_code_type (id, schema_name, lastupdated) FROM stdin;
1	LOINC	2011-06-10 15:31:56.107942-07
2	SNOMED	2011-06-10 15:31:56.107942-07
3	billingCode	2012-06-01 15:10:58.442243-07
4	analyzeCode	2012-06-01 15:10:58.442243-07
\.


--
-- Data for Name: test_dictionary; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_dictionary (id, test_id, dictionary_category_id, context, qualifiable_entry_id, lastupdated) FROM stdin;
\.


--
-- Name: test_dictionary_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.test_dictionary_seq', 1, false);


--
-- Data for Name: test_formats; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_formats (id, lastupdated) FROM stdin;
\.


--
-- Data for Name: test_reflex; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_reflex (id, tst_rslt_id, flags, lastupdated, test_analyte_id, test_id, add_test_id, sibling_reflex, scriptlet_id) FROM stdin;
1	127		2013-10-10 16:53:00.019523	1	56	44	\N	\N
2	130		2013-10-10 16:53:00.019523	2	57	45	\N	\N
3	133		2013-10-10 16:53:00.019523	3	58	46	\N	\N
4	80		2013-10-10 16:53:00.019523	4	44	53	\N	\N
5	85		2013-10-10 16:53:00.019523	5	45	54	\N	\N
6	90		2013-10-10 16:53:00.019523	6	46	55	\N	\N
\.


--
-- Name: test_reflex_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.test_reflex_seq', 6, true);


--
-- Data for Name: test_result; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_result (id, test_id, result_group, flags, tst_rslt_type, value, significant_digits, quant_limit, cont_level, lastupdated, scriptlet_id, sort_order, is_quantifiable, is_active, is_normal) FROM stdin;
11	11	\N	\N	D	542	0	\N	\N	2012-10-24 11:58:25.460291	\N	110	f	t	\N
12	11	\N	\N	D	544	0	\N	\N	2012-10-24 11:58:25.460291	\N	120	f	t	\N
35	31	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	350	f	t	\N
40	32	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	400	f	t	\N
45	33	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	450	f	t	\N
52	36	\N	\N	D	542	0	\N	\N	2012-10-24 11:58:25.460291	\N	520	f	t	\N
53	36	\N	\N	D	544	0	\N	\N	2012-10-24 11:58:25.460291	\N	530	f	t	\N
56	39	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	560	f	t	\N
57	39	\N	\N	D	1105	0	\N	\N	2012-10-24 11:58:25.460291	\N	570	f	t	\N
58	39	\N	\N	D	1106	0	\N	\N	2012-10-24 11:58:25.460291	\N	580	f	t	\N
59	39	\N	\N	D	1107	0	\N	\N	2012-10-24 11:58:25.460291	\N	590	f	t	\N
60	40	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	600	f	t	\N
61	40	\N	\N	D	1105	0	\N	\N	2012-10-24 11:58:25.460291	\N	610	f	t	\N
62	40	\N	\N	D	1106	0	\N	\N	2012-10-24 11:58:25.460291	\N	620	f	t	\N
63	40	\N	\N	D	1107	0	\N	\N	2012-10-24 11:58:25.460291	\N	630	f	t	\N
64	41	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	640	f	t	\N
65	41	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	650	f	t	\N
66	41	\N	\N	D	1105	0	\N	\N	2012-10-24 11:58:25.460291	\N	660	f	t	\N
67	41	\N	\N	D	1108	0	\N	\N	2012-10-24 11:58:25.460291	\N	670	f	t	\N
68	41	\N	\N	D	1107	0	\N	\N	2012-10-24 11:58:25.460291	\N	680	f	t	\N
69	42	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	690	f	t	\N
70	42	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	700	f	t	\N
71	42	\N	\N	D	1105	0	\N	\N	2012-10-24 11:58:25.460291	\N	710	f	t	\N
72	42	\N	\N	D	1108	0	\N	\N	2012-10-24 11:58:25.460291	\N	720	f	t	\N
73	42	\N	\N	D	1107	0	\N	\N	2012-10-24 11:58:25.460291	\N	730	f	t	\N
74	43	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	740	f	t	\N
75	43	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	750	f	t	\N
76	43	\N	\N	D	1105	0	\N	\N	2012-10-24 11:58:25.460291	\N	760	f	t	\N
77	43	\N	\N	D	1108	0	\N	\N	2012-10-24 11:58:25.460291	\N	770	f	t	\N
78	43	\N	\N	D	1107	0	\N	\N	2012-10-24 11:58:25.460291	\N	780	f	t	\N
79	44	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	790	f	t	\N
80	44	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	800	f	t	\N
81	44	\N	\N	D	1105	0	\N	\N	2012-10-24 11:58:25.460291	\N	810	f	t	\N
82	44	\N	\N	D	1108	0	\N	\N	2012-10-24 11:58:25.460291	\N	820	f	t	\N
83	44	\N	\N	D	1107	0	\N	\N	2012-10-24 11:58:25.460291	\N	830	f	t	\N
84	45	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	840	f	t	\N
85	45	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	850	f	t	\N
86	45	\N	\N	D	1105	0	\N	\N	2012-10-24 11:58:25.460291	\N	860	f	t	\N
87	45	\N	\N	D	1108	0	\N	\N	2012-10-24 11:58:25.460291	\N	870	f	t	\N
88	45	\N	\N	D	1107	0	\N	\N	2012-10-24 11:58:25.460291	\N	880	f	t	\N
89	46	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	890	f	t	\N
90	46	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	900	f	t	\N
91	46	\N	\N	D	1105	0	\N	\N	2012-10-24 11:58:25.460291	\N	910	f	t	\N
92	46	\N	\N	D	1108	0	\N	\N	2012-10-24 11:58:25.460291	\N	920	f	t	\N
93	46	\N	\N	D	1107	0	\N	\N	2012-10-24 11:58:25.460291	\N	930	f	t	\N
94	47	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	940	f	t	\N
95	47	\N	\N	D	1063	0	\N	\N	2012-10-24 11:58:25.460291	\N	950	f	t	\N
96	47	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	960	f	t	\N
97	47	\N	\N	D	542	0	\N	\N	2012-10-24 11:58:25.460291	\N	970	f	t	\N
98	48	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	980	f	t	\N
99	48	\N	\N	D	1063	0	\N	\N	2012-10-24 11:58:25.460291	\N	990	f	t	\N
100	48	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	1000	f	t	\N
101	48	\N	\N	D	542	0	\N	\N	2012-10-24 11:58:25.460291	\N	1010	f	t	\N
102	49	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	1020	f	t	\N
103	49	\N	\N	D	1063	0	\N	\N	2012-10-24 11:58:25.460291	\N	1030	f	t	\N
104	49	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	1040	f	t	\N
105	49	\N	\N	D	542	0	\N	\N	2012-10-24 11:58:25.460291	\N	1050	f	t	\N
106	50	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	1060	f	t	\N
107	50	\N	\N	D	1063	0	\N	\N	2012-10-24 11:58:25.460291	\N	1070	f	t	\N
108	50	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	1080	f	t	\N
109	50	\N	\N	D	542	0	\N	\N	2012-10-24 11:58:25.460291	\N	1090	f	t	\N
110	51	\N	\N	D	1063	0	\N	\N	2012-10-24 11:58:25.460291	\N	1100	f	t	\N
111	51	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	1110	f	t	\N
112	51	\N	\N	D	1109	0	\N	\N	2012-10-24 11:58:25.460291	\N	1120	f	t	\N
113	52	\N	\N	D	1063	0	\N	\N	2012-10-24 11:58:25.460291	\N	1130	f	t	\N
114	52	\N	\N	D	1103	0	\N	\N	2012-10-24 11:58:25.460291	\N	1140	f	t	\N
115	52	\N	\N	D	1109	0	\N	\N	2012-10-24 11:58:25.460291	\N	1150	f	t	\N
116	53	\N	\N	D	1000	0	\N	\N	2013-10-10 16:52:59.885338	\N	10	f	t	\N
117	53	\N	\N	D	1103	0	\N	\N	2013-10-10 16:52:59.885338	\N	20	f	t	\N
118	53	\N	\N	D	542	0	\N	\N	2013-10-10 16:52:59.885338	\N	30	f	t	\N
119	54	\N	\N	D	1000	0	\N	\N	2013-10-10 16:52:59.885338	\N	40	f	t	\N
120	54	\N	\N	D	1103	0	\N	\N	2013-10-10 16:52:59.885338	\N	50	f	t	\N
121	54	\N	\N	D	542	0	\N	\N	2013-10-10 16:52:59.885338	\N	60	f	t	\N
122	55	\N	\N	D	1000	0	\N	\N	2013-10-10 16:52:59.885338	\N	70	f	t	\N
123	55	\N	\N	D	1103	0	\N	\N	2013-10-10 16:52:59.885338	\N	80	f	t	\N
124	55	\N	\N	D	542	0	\N	\N	2013-10-10 16:52:59.885338	\N	90	f	t	\N
125	56	\N	\N	D	1000	0	\N	\N	2013-10-10 16:52:59.885338	\N	100	f	t	\N
126	56	\N	\N	D	1103	0	\N	\N	2013-10-10 16:52:59.885338	\N	110	f	t	\N
127	56	\N	\N	D	542	0	\N	\N	2013-10-10 16:52:59.885338	\N	120	f	t	\N
128	57	\N	\N	D	1000	0	\N	\N	2013-10-10 16:52:59.885338	\N	130	f	t	\N
129	57	\N	\N	D	1103	0	\N	\N	2013-10-10 16:52:59.885338	\N	140	f	t	\N
130	57	\N	\N	D	542	0	\N	\N	2013-10-10 16:52:59.885338	\N	150	f	t	\N
131	58	\N	\N	D	1000	0	\N	\N	2013-10-10 16:52:59.885338	\N	160	f	t	\N
132	58	\N	\N	D	1103	0	\N	\N	2013-10-10 16:52:59.885338	\N	170	f	t	\N
133	58	\N	\N	D	542	0	\N	\N	2013-10-10 16:52:59.885338	\N	180	f	t	\N
134	12	\N	\N	D	1103	0	\N	\N	2013-11-07 10:00:30.305117	\N	10	f	t	\N
39	31	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	390	f	t	\N
44	32	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	440	f	t	\N
49	33	\N	\N	D	1000	0	\N	\N	2012-10-24 11:58:25.460291	\N	490	f	t	\N
135	12	\N	\N	D	1100	0	\N	\N	2013-11-07 10:00:30.305117	\N	20	f	t	\N
136	12	\N	\N	D	1101	0	\N	\N	2013-11-07 10:00:30.305117	\N	30	f	t	\N
137	12	\N	\N	D	1102	0	\N	\N	2013-11-07 10:00:30.305117	\N	40	f	t	\N
138	12	\N	\N	D	1103	0	\N	\N	2013-11-07 10:00:30.305117	\N	50	f	t	\N
4	4	\N	\N	N	\N	0	\N	\N	2012-10-24 11:58:25.460291	\N	40	f	t	\N
5	5	\N	\N	N	\N	0	\N	\N	2012-10-24 11:58:25.460291	\N	50	f	t	\N
1	1	\N	\N	N	\N	0	\N	\N	2012-10-24 11:58:25.460291	\N	10	f	t	\N
2	2	\N	\N	N	\N	0	\N	\N	2012-10-24 11:58:25.460291	\N	20	f	t	\N
3	3	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	30	f	t	\N
6	6	\N	\N	N	\N	0	\N	\N	2012-10-24 11:58:25.460291	\N	60	f	t	\N
10	10	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	100	f	t	\N
51	35	\N	\N	N	\N	0	\N	\N	2012-10-24 11:58:25.460291	\N	510	f	t	\N
54	37	\N	\N	N	\N	0	\N	\N	2012-10-24 11:58:25.460291	\N	540	f	t	\N
55	38	\N	\N	N	\N	0	\N	\N	2012-10-24 11:58:25.460291	\N	550	f	t	\N
140	31	\N	\N	D	542	0	\N	\N	2020-01-22 21:46:11.127649	\N	385	f	t	\N
141	32	\N	\N	D	542	0	\N	\N	2020-01-22 21:46:11.127649	\N	435	f	t	\N
142	33	\N	\N	D	542	0	\N	\N	2020-01-22 21:46:11.127649	\N	485	f	t	\N
36	31	\N	\N	D	1060	0	\N	\N	2012-10-24 11:58:25.460291	\N	360	f	f	\N
37	31	\N	\N	D	1061	0	\N	\N	2012-10-24 11:58:25.460291	\N	370	f	f	\N
38	31	\N	\N	D	1062	0	\N	\N	2012-10-24 11:58:25.460291	\N	380	f	f	\N
41	32	\N	\N	D	1060	0	\N	\N	2012-10-24 11:58:25.460291	\N	410	f	f	\N
42	32	\N	\N	D	1061	0	\N	\N	2012-10-24 11:58:25.460291	\N	420	f	f	\N
43	32	\N	\N	D	1062	0	\N	\N	2012-10-24 11:58:25.460291	\N	430	f	f	\N
46	33	\N	\N	D	1060	0	\N	\N	2012-10-24 11:58:25.460291	\N	460	f	f	\N
47	33	\N	\N	D	1061	0	\N	\N	2012-10-24 11:58:25.460291	\N	470	f	f	\N
48	33	\N	\N	D	1062	0	\N	\N	2012-10-24 11:58:25.460291	\N	480	f	f	\N
7	7	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	70	f	t	\N
22	18	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	220	f	t	\N
23	19	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	230	f	t	\N
26	22	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	260	f	t	\N
28	24	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	280	f	t	\N
30	26	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	300	f	t	\N
32	28	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	320	f	t	\N
34	30	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	340	f	t	\N
50	34	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	500	f	t	\N
8	8	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	80	f	t	\N
17	13	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	170	f	t	\N
18	14	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	180	f	t	\N
19	15	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	190	f	t	\N
21	17	\N	\N	N	\N	2	\N	\N	2012-10-24 11:58:25.460291	\N	210	f	t	\N
9	9	\N	\N	N	\N	1	\N	\N	2012-10-24 11:58:25.460291	\N	90	f	t	\N
20	16	\N	\N	N	\N	1	\N	\N	2012-10-24 11:58:25.460291	\N	200	f	t	\N
24	20	\N	\N	N	\N	1	\N	\N	2012-10-24 11:58:25.460291	\N	240	f	t	\N
25	21	\N	\N	N	\N	1	\N	\N	2012-10-24 11:58:25.460291	\N	250	f	t	\N
27	23	\N	\N	N	\N	1	\N	\N	2012-10-24 11:58:25.460291	\N	270	f	t	\N
31	27	\N	\N	N	\N	1	\N	\N	2012-10-24 11:58:25.460291	\N	310	f	t	\N
33	29	\N	\N	N	\N	1	\N	\N	2012-10-24 11:58:25.460291	\N	330	f	t	\N
29	25	\N	\N	N	\N	1	\N	\N	2012-10-24 11:58:25.460291	\N	290	f	t	\N
492	163	8	\N	D	820	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
493	163	8	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
494	163	8	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
495	163	8	\N	D	823	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
496	164	7	\N	D	820	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
497	164	7	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
498	164	7	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
499	164	7	\N	D	823	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
500	165	6	\N	D	824	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
501	165	6	\N	D	825	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
502	165	6	\N	D	826	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
503	165	6	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
504	165	6	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
505	165	6	\N	D	829	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
506	166	5	\N	D	824	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
507	166	5	\N	D	825	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
508	166	5	\N	D	826	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
509	166	5	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
510	166	5	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
511	166	5	\N	D	829	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
512	167	4	\N	D	826	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
513	167	4	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
514	167	4	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
515	167	4	\N	D	829	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
516	168	3	\N	D	820	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
517	168	3	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
520	169	2	\N	D	820	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
521	169	2	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
522	169	2	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
523	169	2	\N	D	823	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
524	170	1	\N	D	820	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
525	170	1	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
526	170	1	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
527	170	1	\N	D	823	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
528	167	4	\N	D	824	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
529	167	4	\N	D	825	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
533	162	14	\N	D	860	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
534	162	14	\N	D	1171	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
535	162	14	\N	D	1172	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
536	173	18	\N	R	\N	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
537	175	18	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
538	175	18	\N	D	823	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
539	175	18	\N	D	820	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
540	175	18	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
541	174	19	\N	A	\N	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
565	198	\N	\N	D	824	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
566	198	\N	\N	D	825	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
567	198	\N	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
569	198	\N	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
570	198	\N	\N	D	829	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
571	199	20	\N	D	824	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
572	199	20	\N	D	825	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
573	199	20	\N	D	826	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
574	199	20	\N	D	821	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
575	199	20	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
576	199	20	\N	D	829	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
553	187	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
547	172	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
518	168	3	\N	D	822	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	f	\N
519	168	3	\N	D	823	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	f	\N
530	191	17	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
531	177	16	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
542	193	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
543	194	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
544	195	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
545	196	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
546	197	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
548	179	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
532	180	15	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
549	181	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
550	182	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
551	183	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
552	186	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
577	175	18	\N	D	1178	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
562	159	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
563	190	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
564	192	\N	\N	N	\N	2	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
554	184	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
557	185	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
555	188	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
556	189	\N	\N	N	\N	1	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
561	160	\N	\N	N	\N	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
560	176	\N	\N	N	\N	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
559	161	\N	\N	N	\N	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
558	178	\N	\N	N	\N	0	\N	\N	2020-01-22 21:46:41.964739	\N	\N	f	t	\N
\.


--
-- Name: test_result_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.test_result_seq', 142, true);


--
-- Data for Name: test_section; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_section (id, name, description, org_id, is_external, lastupdated, parent_test_section, sort_order, is_active, name_localization_id, display_key) FROM stdin;
156	user	Indicates user will chose test section	\N	N	2013-01-11 14:28:05.971827	\N	2147483647	N	125	\N
136	Molecular Biology	Biologie Moleculaire	\N	N	2012-10-24 11:58:25.242079	\N	50	Y	131	\N
160	EID	Virologie_EID	\N	N	2020-01-22 21:46:41.338786	\N	2147483647	N	400	testSection.EID
161	VL	Virologie_VL	\N	N	2020-01-22 21:46:41.338786	\N	2147483647	N	401	testSection.VL
58	Parasitology	Parasitology logbook	3	N	2009-02-12 16:50:45.078	\N	2147483647	N	120	testSection.Parasitologie
60	ECBU	ECBU logbook	3	N	2009-02-12 16:51:58.875	\N	2147483647	N	121	testSection.ECBU
56	Biochemistry	Chem logbook	3	N	2009-07-23 12:57:40.365	\N	10	Y	127	testSection.Biochimie
36	Hematology	Hematology logbook	3	N	2009-02-12 16:52:58.437	\N	20	Y	128	testSection.Hematologie
59	Immunology	Immuno-Serology logbook	3	N	2009-02-12 16:51:30.203	\N	40	Y	130	testsection.Immunologie
61	VCT	VCT logbook	3	N	2009-02-12 16:52:31.906	\N	2147483647	N	122	testSection.VCT
57	Bacteria	Bacteria logbook	3	N	2009-02-12 16:49:59.468	\N	2147483647	N	119	testSection.Bacteriologie
96	Mycobacteriology	Mycobacteriology	3	N	2011-04-05 10:46:47.646357	\N	2147483647	N	124	testSection.Mycobacteriologie
117	Serology-Immunology	Serology-Immunology test section	3	N	2012-06-01 15:10:58.553362	\N	30	Y	129	testSection.Serology-Immunologie
116	Hemto-Immunology	Hemto-Immunology test section	3	N	2012-06-01 15:10:58.53984	\N	2147483647	N	126	testSection.Hemato_Immunologie
97	Serology	Serology Department	3	N	2020-01-22 21:46:41.941042	\N	2147483647	Y	280	testSection.Serologie
76	Virologie	Virologie	3	N	2010-01-12 11:54:36.75	\N	2147483647	Y	123	testSection.Virologie
\.


--
-- Name: test_section_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.test_section_seq', 150, false);


--
-- Name: test_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.test_seq', 300, false);


--
-- Data for Name: test_trailer; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_trailer (id, name, description, text, lastupdated) FROM stdin;
8	Diane Test	Diane Test	Diane Test	2006-09-28 09:14:04.334
12	test trailer	test trailer	this is test trailer comment	2006-09-27 09:09:24.34
13	CULTP	Culture in Progress	Smear prepared from concentrated specimen. Culture testing in progress. A separate report will be sent to you when complete.	2006-11-08 11:32:47.789
14	WARNP	WARNING: PCR results are FOR RESEARCH	WARNING: PCR results are FOR RESEARCH USE ONLY and should not be used for diagnosis of disease. Patient management decisions must be based on appropriate findings by a physician and the clinical condition of the patient.	2007-02-05 09:09:21.726
15	XML Test	This is test_trailer description <!field: source of comment	This is test_trailer text field. <It! is the actual comment	2008-05-01 21:36:26.312
1	RESEARCH	RESEARCH	This test is for research use only.	2007-05-07 14:38:55.661
\.


--
-- Name: test_trailer_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.test_trailer_seq', 2, false);


--
-- Data for Name: test_worksheet_item; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_worksheet_item (id, tw_id, qc_id, "position", cell_type) FROM stdin;
\.


--
-- Data for Name: test_worksheets; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.test_worksheets (id, test_id, batch_capacity, total_capacity, number_format) FROM stdin;
\.


--
-- Name: tobereomved_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.tobereomved_seq', 1, false);


--
-- Data for Name: type_of_data_indicator; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.type_of_data_indicator (id, name, name_key, description, description_key, lastupdated) FROM stdin;
1	Turnaround Time	datasubmission.tat	Difference of days between date of report generation date and interview date	datasubmission.tat.desc	2020-01-22 21:46:54.210577
2	VL Coverage	datasubmission.vlcov	Count of suppressed and unsuppresed divided by a reference (totalartmar)	datasubmission.vlcov.desc	2020-01-22 21:46:54.210577
3	VL Outcomes	datasubmission.vloutcomes		datasubmission.vloutcomes.desc	2020-01-22 21:46:54.210577
4	Testing Trends	datasubmission.testtrends	number of tests done	datasubmission.testtrends.desc	2020-01-22 21:46:54.210577
5	Gender Trends	datasubmission.gendertrend	number of tests done by gender	datasubmission.gendertrend.desc	2020-01-22 21:46:54.210577
6	Age Trends	datasubmission.agetrends	number of tests done by age	datasubmission.agetrends.desc	2020-01-22 21:46:54.210577
7	Justification	datasubmission.reason	number of tests done by reason	datasubmission.reason.desc	2020-01-22 21:46:54.210577
8	Gender Suppression	datasubmission.gendersuppress	suppression by gender	datasubmission.gendersuppress.desc	2020-01-22 21:46:54.210577
9	Age Suppression	datasubmission.agesuppress	suppression by age	datasubmission.agesuppress.desc	2020-01-22 21:46:54.210577
\.


--
-- Name: type_of_data_indicator_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.type_of_data_indicator_seq', 9, true);


--
-- Data for Name: type_of_provider; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.type_of_provider (id, description, tp_code) FROM stdin;
\.


--
-- Data for Name: type_of_sample; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.type_of_sample (id, description, domain, lastupdated, local_abbrev, is_active, sort_order, name_localization_id, display_key) FROM stdin;
5	Actual type will be selected by user	H	2013-09-19 14:31:47.008874	Variable	f	2147483647	136	\N
3	Plasma	H	2012-10-24 11:58:25.215565	Plasma	t	20	137	sample.type.Plasma
2	Serum	H	2012-10-24 11:58:25.215565	Serum	t	10	139	sample.type.Serum
4	Whole Blood	H	2012-10-24 11:58:25.215565	Whole Bld	t	50	138	sample.type.Sang
1	Urines	H	2012-10-24 11:58:25.215565	Urines	t	30	140	sample.type.Urines
24	Dry Tube	H	2020-01-22 21:46:41.326636	Dry	t	2147483647	141	sample.type.dryTube
25	EDTA Tube	H	2020-01-22 21:46:41.326636	EDTA	t	2147483647	142	sample.type.edtaTube
26	DBS	H	2020-01-22 21:46:41.326636	DBS	t	2147483647	143	sample.type.DBS
\.


--
-- Name: type_of_sample_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.type_of_sample_seq', 30, false);


--
-- Data for Name: type_of_test_result; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.type_of_test_result (id, test_result_type, description, lastupdated, hl7_value) FROM stdin;
2	D	Dictionary	2006-11-08 11:40:58.824	TX
3	T	Titer	2006-03-29 11:53:15	TX
4	N	Numeric	2006-03-29 11:53:21	NM
1	R	Remark	2010-10-28 06:12:41.971687	TX
5	A	Alpha,no range check	2010-10-28 06:13:53.177655	TX
6	M	Multiselect	2011-01-06 10:57:15.79331	TX
7	C	Cascading Multiselect	2014-03-26 12:27:07.746357	TX
\.


--
-- Name: type_of_test_result_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.type_of_test_result_seq', 7, true);


--
-- Data for Name: unit_of_measure; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.unit_of_measure (id, name, description, lastupdated) FROM stdin;
1	ppl	ppl	2008-05-01 21:13:14.164
5	%	%	2009-04-16 17:00:28.769
7	mm3	mm3	2009-04-16 17:00:49.371
6	ppm	ppm	2009-04-16 17:00:57.823
9	mlU/ml	mlU/ml	2009-07-29 17:23:59.219
10	u/L	u/L	2009-07-29 17:24:14.382
11	ug/dL	ug/dL	2009-07-29 17:24:30.403
19	cp/mL	cp/mL	2009-07-29 17:59:54.673
12	g/dl	g/dl	2009-07-29 18:00:11.255
8	mg/dl	mg/dl	2009-07-29 18:00:40.864
13	million/uL	million/uL	2009-07-29 18:01:27.914
17	mns	mns	2009-07-29 18:01:50.581
16	pg	pg	2009-07-29 18:01:59.005
20	ui/ml	ui/ml	2009-07-30 11:27:36.658
21	mU/ml	mU/ml	2009-07-30 11:34:52.814
22	mm/h	mm/h	2009-07-30 12:13:36.267
15	K/mm^3	K/mm^3	2009-07-30 12:27:35.83
18	micron^3	microns^3	2009-07-30 12:27:53.517
14	mille/mm^3	mille/mm^3	2009-07-30 12:29:11.549
23	Vol%	Vol%	2009-08-02 17:00:51.909
24	million/mm^3	million/mm^3	2009-08-02 17:10:58.106
25	g/l	g/l	2011-01-04 14:21:42.926
26	Ul/l	UI/l	2011-01-04 14:35:10.508
27	mg/l	mg/l	2011-01-13 16:13:28.43
28	ug/l	ug/l	2011-02-03 16:15:48.253
29	UI/L	UI/L	2012-06-01 15:10:58.580442
30	copies/ml	copies/ml	2012-06-01 15:10:58.580442
31	mille/mm3	mille/mm3	2012-06-01 15:10:58.580442
32	fl	fl	2012-06-01 15:10:58.580442
33	/mm3	/mm3	2012-06-01 15:10:58.580442
34	Copies/ml	Copies/ml	2012-06-01 15:10:58.580442
35	μl	μl	2012-06-01 15:10:58.580442
36	million/mm3	million/mm3	2012-10-24 11:58:24.806274
37	g/dL	g/dL	2020-01-22 21:46:11.101921
45	Cell/µl	Cell/µl	2020-01-22 21:46:41.924926
39	10^3/µl	10^3/µl	2020-01-22 21:46:41.924926
41	10^6/µl	10^6/µl	2020-01-22 21:46:41.924926
\.


--
-- Name: unit_of_measure_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.unit_of_measure_seq', 160, false);


--
-- Data for Name: user_alert_map; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.user_alert_map (user_id, alert_id, report_id, alert_limit, alert_operator, map_id) FROM stdin;
\.


--
-- Data for Name: user_group_map; Type: TABLE DATA; Schema: clinlims; Owner: clinlims
--

COPY clinlims.user_group_map (user_id, group_id, map_id) FROM stdin;
1	1120	0
241	1121	0
\.


--
-- Name: zip_code_seq; Type: SEQUENCE SET; Schema: clinlims; Owner: clinlims
--

SELECT pg_catalog.setval('clinlims.zip_code_seq', 1, false);


--
-- Name: address_part_part_name_key; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.address_part
    ADD CONSTRAINT address_part_part_name_key UNIQUE (part_name);


--
-- Name: address_part_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.address_part
    ADD CONSTRAINT address_part_pk PRIMARY KEY (id);


--
-- Name: analysis_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis
    ADD CONSTRAINT analysis_pk PRIMARY KEY (id);


--
-- Name: analyte_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyte
    ADD CONSTRAINT analyte_pk PRIMARY KEY (id);


--
-- Name: analyzer_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyzer
    ADD CONSTRAINT analyzer_pk PRIMARY KEY (id);


--
-- Name: analyzer_result_status_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyzer_result_status
    ADD CONSTRAINT analyzer_result_status_pk PRIMARY KEY (id);


--
-- Name: analyzer_test_map_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyzer_test_map
    ADD CONSTRAINT analyzer_test_map_pk PRIMARY KEY (analyzer_id, analyzer_test_name);


--
-- Name: anqaev_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis_qaevent
    ADD CONSTRAINT anqaev_pk PRIMARY KEY (id);


--
-- Name: attachment_item_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.attachment_item
    ADD CONSTRAINT attachment_item_pk PRIMARY KEY (id);


--
-- Name: attachment_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.attachment
    ADD CONSTRAINT attachment_pk PRIMARY KEY (id);


--
-- Name: barcode_label_info_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.barcode_label_info
    ADD CONSTRAINT barcode_label_info_pkey PRIMARY KEY (id);


--
-- Name: code_element_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.code_element_type
    ADD CONSTRAINT code_element_type_pk PRIMARY KEY (id);


--
-- Name: code_element_xref_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.code_element_xref
    ADD CONSTRAINT code_element_xref_pk PRIMARY KEY (id);


--
-- Name: cron_scheduler_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.quartz_cron_scheduler
    ADD CONSTRAINT cron_scheduler_pk PRIMARY KEY (id);


--
-- Name: data_indicator_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.data_indicator
    ADD CONSTRAINT data_indicator_pkey PRIMARY KEY (id);


--
-- Name: data_resource_level_id_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.data_resource_level_id
    ADD CONSTRAINT data_resource_level_id_pkey PRIMARY KEY (id);


--
-- Name: data_resource_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.data_resource
    ADD CONSTRAINT data_resource_pkey PRIMARY KEY (id);


--
-- Name: data_value_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.data_value
    ADD CONSTRAINT data_value_pkey PRIMARY KEY (id);


--
-- Name: demog_hist_type_type_name_u; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.observation_history_type
    ADD CONSTRAINT demog_hist_type_type_name_u UNIQUE (type_name);


--
-- Name: demographic_history_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.observation_history_type
    ADD CONSTRAINT demographic_history_type_pk PRIMARY KEY (id);


--
-- Name: demographics_history_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.observation_history
    ADD CONSTRAINT demographics_history_pk PRIMARY KEY (id);


--
-- Name: dict_cat_desc_u; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.dictionary_category
    ADD CONSTRAINT dict_cat_desc_u UNIQUE (description);


--
-- Name: dict_cat_local_abbrev_u; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.dictionary_category
    ADD CONSTRAINT dict_cat_local_abbrev_u UNIQUE (local_abbrev);


--
-- Name: dict_dict_cat_id_dict_entry_u; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.dictionary
    ADD CONSTRAINT dict_dict_cat_id_dict_entry_u UNIQUE (dictionary_category_id, dict_entry);


--
-- Name: dict_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.dictionary
    ADD CONSTRAINT dict_pk PRIMARY KEY (id);


--
-- Name: dictionary_category_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.dictionary_category
    ADD CONSTRAINT dictionary_category_pk PRIMARY KEY (id);


--
-- Name: electronic_order_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.electronic_order
    ADD CONSTRAINT electronic_order_pk PRIMARY KEY (id);


--
-- Name: env_samp_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_environmental
    ADD CONSTRAINT env_samp_pk PRIMARY KEY (id);


--
-- Name: gender_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.gender
    ADD CONSTRAINT gender_pk PRIMARY KEY (id);


--
-- Name: hist_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.history
    ADD CONSTRAINT hist_pk PRIMARY KEY (id);


--
-- Name: hl7_encoding_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_code_type
    ADD CONSTRAINT hl7_encoding_type_pk PRIMARY KEY (id);


--
-- Name: hl7_message_out_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.hl7_message_out
    ADD CONSTRAINT hl7_message_out_pkey PRIMARY KEY (id);


--
-- Name: hum_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_human
    ADD CONSTRAINT hum_pk PRIMARY KEY (id);


--
-- Name: ia_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.instrument_analyte
    ADD CONSTRAINT ia_pk PRIMARY KEY (id);


--
-- Name: id; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyzer_results
    ADD CONSTRAINT id PRIMARY KEY (id);


--
-- Name: identity_type_uk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_identity_type
    ADD CONSTRAINT identity_type_uk UNIQUE (identity_type);


--
-- Name: il_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.instrument_log
    ADD CONSTRAINT il_pk PRIMARY KEY (id);


--
-- Name: image_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.image
    ADD CONSTRAINT image_pk PRIMARY KEY (id);


--
-- Name: instru_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.instrument
    ADD CONSTRAINT instru_pk PRIMARY KEY (id);


--
-- Name: inv_recpt_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_receipt
    ADD CONSTRAINT inv_recpt_pk PRIMARY KEY (id);


--
-- Name: invcomp_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_component
    ADD CONSTRAINT invcomp_pk PRIMARY KEY (id);


--
-- Name: invitem_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_item
    ADD CONSTRAINT invitem_pk PRIMARY KEY (id);


--
-- Name: invloc_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_location
    ADD CONSTRAINT invloc_pk PRIMARY KEY (id);


--
-- Name: label_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.label
    ADD CONSTRAINT label_pk PRIMARY KEY (id);


--
-- Name: localization_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.localization
    ADD CONSTRAINT localization_pkey PRIMARY KEY (id);


--
-- Name: login_user_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.login_user
    ADD CONSTRAINT login_user_pk PRIMARY KEY (login_name);


--
-- Name: ma_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.method_analyte
    ADD CONSTRAINT ma_pk PRIMARY KEY (id);


--
-- Name: menu_element_id_key; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.menu
    ADD CONSTRAINT menu_element_id_key UNIQUE (element_id);


--
-- Name: message_org_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.message_org
    ADD CONSTRAINT message_org_pk PRIMARY KEY (id);


--
-- Name: method_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.method
    ADD CONSTRAINT method_pk PRIMARY KEY (id);


--
-- Name: methres_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.method_result
    ADD CONSTRAINT methres_pk PRIMARY KEY (id);


--
-- Name: mls_lab_tp_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.mls_lab_type
    ADD CONSTRAINT mls_lab_tp_pk PRIMARY KEY (id);


--
-- Name: newborn_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_newborn
    ADD CONSTRAINT newborn_pk PRIMARY KEY (id);


--
-- Name: note_id; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.note
    ADD CONSTRAINT note_id PRIMARY KEY (id);


--
-- Name: or_properties_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.or_properties
    ADD CONSTRAINT or_properties_pkey PRIMARY KEY (property_id);


--
-- Name: or_properties_property_key_key; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.or_properties
    ADD CONSTRAINT or_properties_property_key_key UNIQUE (property_key);


--
-- Name: or_tags_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.or_tags
    ADD CONSTRAINT or_tags_pkey PRIMARY KEY (tag_id);


--
-- Name: ord_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.orders
    ADD CONSTRAINT ord_pk PRIMARY KEY (id);


--
-- Name: orditem_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.order_item
    ADD CONSTRAINT orditem_pk PRIMARY KEY (id);


--
-- Name: org_contact_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_contact
    ADD CONSTRAINT org_contact_pk PRIMARY KEY (id);


--
-- Name: org_hl7_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.org_hl7_encoding_type
    ADD CONSTRAINT org_hl7_pk PRIMARY KEY (organization_id, encoding_type_id);


--
-- Name: org_mlt_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.org_mls_lab_type
    ADD CONSTRAINT org_mlt_pk PRIMARY KEY (org_mlt_id);


--
-- Name: org_org_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_organization_type
    ADD CONSTRAINT org_org_type_pk PRIMARY KEY (org_id, org_type_id);


--
-- Name: org_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization
    ADD CONSTRAINT org_pk PRIMARY KEY (id);


--
-- Name: org_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_type
    ADD CONSTRAINT org_type_pk PRIMARY KEY (id);


--
-- Name: organization_address_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_address
    ADD CONSTRAINT organization_address_pk PRIMARY KEY (organization_id, address_part_id);


--
-- Name: pac_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.package_1
    ADD CONSTRAINT pac_pk PRIMARY KEY (id);


--
-- Name: pan_it_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.panel_item
    ADD CONSTRAINT pan_it_pk PRIMARY KEY (id);


--
-- Name: panel_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.panel
    ADD CONSTRAINT panel_pk PRIMARY KEY (id);


--
-- Name: pat_ident_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_identity_type
    ADD CONSTRAINT pat_ident_type_pk PRIMARY KEY (id);


--
-- Name: pat_identity_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_identity
    ADD CONSTRAINT pat_identity_pk PRIMARY KEY (id);


--
-- Name: pat_pat_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_patient_type
    ADD CONSTRAINT pat_pat_type_pk PRIMARY KEY (id);


--
-- Name: pat_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient
    ADD CONSTRAINT pat_pk PRIMARY KEY (id);


--
-- Name: pat_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_type
    ADD CONSTRAINT pat_type_pk PRIMARY KEY (id);


--
-- Name: pay_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.payment_type
    ADD CONSTRAINT pay_type_pk PRIMARY KEY (id);


--
-- Name: person_address_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.person_address
    ADD CONSTRAINT person_address_pk PRIMARY KEY (person_id, address_part_id);


--
-- Name: person_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.person
    ADD CONSTRAINT person_pk PRIMARY KEY (id);


--
-- Name: pk_document_track; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.document_track
    ADD CONSTRAINT pk_document_track PRIMARY KEY (id);


--
-- Name: pk_document_type; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.document_type
    ADD CONSTRAINT pk_document_type PRIMARY KEY (id);


--
-- Name: pk_menu; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.menu
    ADD CONSTRAINT pk_menu PRIMARY KEY (id);


--
-- Name: pk_nc_event; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nc_event
    ADD CONSTRAINT pk_nc_event PRIMARY KEY (id);


--
-- Name: pk_nce_action_log; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nce_action_log
    ADD CONSTRAINT pk_nce_action_log PRIMARY KEY (id);


--
-- Name: pk_nce_category; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nce_category
    ADD CONSTRAINT pk_nce_category PRIMARY KEY (id);


--
-- Name: pk_nce_specimen; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nce_specimen
    ADD CONSTRAINT pk_nce_specimen PRIMARY KEY (id);


--
-- Name: pk_nce_type; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nce_type
    ADD CONSTRAINT pk_nce_type PRIMARY KEY (id);


--
-- Name: pk_qa_observation; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qa_observation
    ADD CONSTRAINT pk_qa_observation PRIMARY KEY (id);


--
-- Name: pk_qa_observation_type; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qa_observation_type
    ADD CONSTRAINT pk_qa_observation_type PRIMARY KEY (id);


--
-- Name: pk_report; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.report
    ADD CONSTRAINT pk_report PRIMARY KEY (id);


--
-- Name: pk_test_dictionary; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_dictionary
    ADD CONSTRAINT pk_test_dictionary PRIMARY KEY (id);


--
-- Name: pr_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_relations
    ADD CONSTRAINT pr_pk PRIMARY KEY (id);


--
-- Name: progs_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.program
    ADD CONSTRAINT progs_pk PRIMARY KEY (id);


--
-- Name: proj_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.project
    ADD CONSTRAINT proj_pk PRIMARY KEY (id);


--
-- Name: project_org_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.project_organization
    ADD CONSTRAINT project_org_pk PRIMARY KEY (project_id, org_id);


--
-- Name: projparam_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.project_parameter
    ADD CONSTRAINT projparam_pk PRIMARY KEY (id);


--
-- Name: provider_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.provider
    ADD CONSTRAINT provider_pk PRIMARY KEY (id);


--
-- Name: qa_event_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qa_event
    ADD CONSTRAINT qa_event_pk PRIMARY KEY (id);


--
-- Name: qc_analyt_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qc_analytes
    ADD CONSTRAINT qc_analyt_pk PRIMARY KEY (id);


--
-- Name: qc_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qc
    ADD CONSTRAINT qc_pk PRIMARY KEY (id);


--
-- Name: qrtz_blob_triggers_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (trigger_name, trigger_group);


--
-- Name: qrtz_calendars_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_calendars
    ADD CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (calendar_name);


--
-- Name: qrtz_cron_triggers_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (trigger_name, trigger_group);


--
-- Name: qrtz_fired_triggers_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_fired_triggers
    ADD CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (entry_id);


--
-- Name: qrtz_job_details_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_job_details
    ADD CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (job_name, job_group);


--
-- Name: qrtz_job_listeners_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_job_listeners
    ADD CONSTRAINT qrtz_job_listeners_pkey PRIMARY KEY (job_name, job_group, job_listener);


--
-- Name: qrtz_locks_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_locks
    ADD CONSTRAINT qrtz_locks_pkey PRIMARY KEY (lock_name);


--
-- Name: qrtz_paused_trigger_grps_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_paused_trigger_grps
    ADD CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (trigger_group);


--
-- Name: qrtz_scheduler_state_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_scheduler_state
    ADD CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (instance_name);


--
-- Name: qrtz_simple_triggers_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (trigger_name, trigger_group);


--
-- Name: qrtz_trigger_listeners_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_trigger_listeners
    ADD CONSTRAINT qrtz_trigger_listeners_pkey PRIMARY KEY (trigger_name, trigger_group, trigger_listener);


--
-- Name: qrtz_triggers_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (trigger_name, trigger_group);


--
-- Name: receiver_code_element_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.receiver_code_element
    ADD CONSTRAINT receiver_code_element_pk PRIMARY KEY (id);


--
-- Name: referral_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral
    ADD CONSTRAINT referral_pk PRIMARY KEY (id);


--
-- Name: referral_reason_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral_reason
    ADD CONSTRAINT referral_reason_pk PRIMARY KEY (id);


--
-- Name: referral_result_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral_result
    ADD CONSTRAINT referral_result_pk PRIMARY KEY (id);


--
-- Name: referral_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral_type
    ADD CONSTRAINT referral_type_pk PRIMARY KEY (id);


--
-- Name: referring_test_result_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referring_test_result
    ADD CONSTRAINT referring_test_result_pk PRIMARY KEY (id);


--
-- Name: region_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.region
    ADD CONSTRAINT region_pk PRIMARY KEY (id);


--
-- Name: report_external_import_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.report_external_import
    ADD CONSTRAINT report_external_import_pk PRIMARY KEY (id);


--
-- Name: report_queue_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.report_external_export
    ADD CONSTRAINT report_queue_pk PRIMARY KEY (id);


--
-- Name: report_queue_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.report_queue_type
    ADD CONSTRAINT report_queue_type_pk PRIMARY KEY (id);


--
-- Name: requester_type_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.requester_type
    ADD CONSTRAINT requester_type_pk PRIMARY KEY (id);


--
-- Name: result_inventory_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_inventory
    ADD CONSTRAINT result_inventory_pk PRIMARY KEY (id);


--
-- Name: result_limits_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_limits
    ADD CONSTRAINT result_limits_pk PRIMARY KEY (id);


--
-- Name: result_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result
    ADD CONSTRAINT result_pk PRIMARY KEY (id);


--
-- Name: result_signature_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_signature
    ADD CONSTRAINT result_signature_pk PRIMARY KEY (id);


--
-- Name: rt_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.reference_tables
    ADD CONSTRAINT rt_pk PRIMARY KEY (id);


--
-- Name: samp_org_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_organization
    ADD CONSTRAINT samp_org_pk PRIMARY KEY (id);


--
-- Name: samp_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample
    ADD CONSTRAINT samp_pk PRIMARY KEY (id);


--
-- Name: sampitem_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_item
    ADD CONSTRAINT sampitem_pk PRIMARY KEY (id);


--
-- Name: sample_human_samp_id_u; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_human
    ADD CONSTRAINT sample_human_samp_id_u UNIQUE (samp_id);


--
-- Name: sample_pdf_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_pdf
    ADD CONSTRAINT sample_pdf_pk PRIMARY KEY (id);


--
-- Name: sample_projects_pk_i; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_projects
    ADD CONSTRAINT sample_projects_pk_i PRIMARY KEY (id);


--
-- Name: sample_qaevent_action_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_qaevent_action
    ADD CONSTRAINT sample_qaevent_action_pkey PRIMARY KEY (id);


--
-- Name: sample_qaevent_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_qaevent
    ADD CONSTRAINT sample_qaevent_pkey PRIMARY KEY (id);


--
-- Name: sample_requester_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_requester
    ADD CONSTRAINT sample_requester_pk PRIMARY KEY (id);


--
-- Name: sampledom_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_domain
    ADD CONSTRAINT sampledom_pk PRIMARY KEY (id);


--
-- Name: sampletype_panel_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sampletype_panel
    ADD CONSTRAINT sampletype_panel_pkey PRIMARY KEY (id);


--
-- Name: sampletype_test_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sampletype_test
    ADD CONSTRAINT sampletype_test_pkey PRIMARY KEY (id);


--
-- Name: scr_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.state_code
    ADD CONSTRAINT scr_pk PRIMARY KEY (id);


--
-- Name: scrip_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.scriptlet
    ADD CONSTRAINT scrip_pk PRIMARY KEY (id);


--
-- Name: site_info_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.site_information
    ADD CONSTRAINT site_info_pk PRIMARY KEY (id);


--
-- Name: site_information_domain_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.site_information_domain
    ADD CONSTRAINT site_information_domain_pk PRIMARY KEY (id);


--
-- Name: source_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.source_of_sample
    ADD CONSTRAINT source_pk PRIMARY KEY (id);


--
-- Name: storage_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.storage_location
    ADD CONSTRAINT storage_pk PRIMARY KEY (id);


--
-- Name: su_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.storage_unit
    ADD CONSTRAINT su_pk PRIMARY KEY (id);


--
-- Name: sys_c003998; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.action
    ADD CONSTRAINT sys_c003998 PRIMARY KEY (id);


--
-- Name: sys_c003999; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.action
    ADD CONSTRAINT sys_c003999 UNIQUE (code);


--
-- Name: sys_c004009; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis_qaevent_action
    ADD CONSTRAINT sys_c004009 PRIMARY KEY (id);


--
-- Name: sys_c004307; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.status_of_sample
    ADD CONSTRAINT sys_c004307 PRIMARY KEY (id);


--
-- Name: sys_mod_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_module
    ADD CONSTRAINT sys_mod_pk PRIMARY KEY (id);


--
-- Name: sys_role_mo_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_role_module
    ADD CONSTRAINT sys_role_mo_pk PRIMARY KEY (id);


--
-- Name: sys_use_mo_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_module
    ADD CONSTRAINT sys_use_mo_pk PRIMARY KEY (id);


--
-- Name: sys_use_se_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_section
    ADD CONSTRAINT sys_use_se_pk PRIMARY KEY (id);


--
-- Name: sys_user_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user
    ADD CONSTRAINT sys_user_pk PRIMARY KEY (id);


--
-- Name: sys_usr_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_role
    ADD CONSTRAINT sys_usr_pk PRIMARY KEY (system_user_id, role_id);


--
-- Name: system_module_param_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_module_param
    ADD CONSTRAINT system_module_param_pkey PRIMARY KEY (id);


--
-- Name: system_module_url_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_module_url
    ADD CONSTRAINT system_module_url_pkey PRIMARY KEY (id);


--
-- Name: system_role_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_role
    ADD CONSTRAINT system_role_pkey PRIMARY KEY (id);


--
-- Name: test_analyte_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_analyte
    ADD CONSTRAINT test_analyte_pk PRIMARY KEY (id);


--
-- Name: test_hl7_code_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_code
    ADD CONSTRAINT test_hl7_code_pk PRIMARY KEY (test_id, code_type_id);


--
-- Name: test_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT test_pk PRIMARY KEY (id);


--
-- Name: test_reflx_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_reflex
    ADD CONSTRAINT test_reflx_pk PRIMARY KEY (id);


--
-- Name: test_sect_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_section
    ADD CONSTRAINT test_sect_pk PRIMARY KEY (id);


--
-- Name: testfrmt_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_formats
    ADD CONSTRAINT testfrmt_pk PRIMARY KEY (id);


--
-- Name: tst_rslt_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_result
    ADD CONSTRAINT tst_rslt_pk PRIMARY KEY (id);


--
-- Name: tsttrlr_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_trailer
    ADD CONSTRAINT tsttrlr_pk PRIMARY KEY (id);


--
-- Name: tw_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_worksheets
    ADD CONSTRAINT tw_pk PRIMARY KEY (id);


--
-- Name: twi_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_worksheet_item
    ADD CONSTRAINT twi_pk PRIMARY KEY (id);


--
-- Name: type_of_data_indicator_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.type_of_data_indicator
    ADD CONSTRAINT type_of_data_indicator_pkey PRIMARY KEY (id);


--
-- Name: type_of_test_result_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.type_of_test_result
    ADD CONSTRAINT type_of_test_result_pk PRIMARY KEY (id);


--
-- Name: typeosamp_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.type_of_sample
    ADD CONSTRAINT typeosamp_pk PRIMARY KEY (id);


--
-- Name: typofprovider_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.type_of_provider
    ADD CONSTRAINT typofprovider_pk PRIMARY KEY (id);


--
-- Name: unique_guid; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT unique_guid UNIQUE (guid);


--
-- Name: uom_pk; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.unit_of_measure
    ADD CONSTRAINT uom_pk PRIMARY KEY (id);


--
-- Name: user_alert_map_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.user_alert_map
    ADD CONSTRAINT user_alert_map_pkey PRIMARY KEY (user_id, map_id);


--
-- Name: user_group_map_pkey; Type: CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.user_group_map
    ADD CONSTRAINT user_group_map_pkey PRIMARY KEY (user_id, map_id);


--
-- Name: accnum_uk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX accnum_uk ON clinlims.sample USING btree (accession_number);


--
-- Name: analysis_sampitem_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX analysis_sampitem_fk_i ON clinlims.analysis USING btree (sampitem_id);


--
-- Name: analysis_test_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX analysis_test_fk_i ON clinlims.analysis USING btree (test_id);


--
-- Name: analysis_test_sect_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX analysis_test_sect_fk_i ON clinlims.analysis USING btree (test_sect_id);


--
-- Name: analyte_analyte_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX analyte_analyte_fk_i ON clinlims.analyte USING btree (analyte_id);


--
-- Name: anqaev_anal_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX anqaev_anal_fk_i ON clinlims.analysis_qaevent USING btree (analysis_id);


--
-- Name: anqaev_qa_event_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX anqaev_qa_event_fk_i ON clinlims.analysis_qaevent USING btree (qa_event_id);


--
-- Name: attachmtitem_attachmt_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX attachmtitem_attachmt_fk_i ON clinlims.attachment_item USING btree (attachment_id);


--
-- Name: env_samp_samp_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX env_samp_samp_fk_i ON clinlims.sample_environmental USING btree (samp_id);


--
-- Name: hist_sys_user_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX hist_sys_user_fk_i ON clinlims.history USING btree (sys_user_id);


--
-- Name: hist_table_row_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX hist_table_row_i ON clinlims.history USING btree (reference_id, reference_table);


--
-- Name: hum_pat_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX hum_pat_fk_i ON clinlims.sample_human USING btree (patient_id);


--
-- Name: hum_provider_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX hum_provider_fk_i ON clinlims.sample_human USING btree (provider_id);


--
-- Name: hum_samp_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX hum_samp_fk_i ON clinlims.sample_human USING btree (samp_id);


--
-- Name: ia_analyte_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX ia_analyte_fk_i ON clinlims.instrument_analyte USING btree (analyte_id);


--
-- Name: ia_instru_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX ia_instru_fk_i ON clinlims.instrument_analyte USING btree (instru_id);


--
-- Name: ia_method_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX ia_method_fk_i ON clinlims.instrument_analyte USING btree (method_id);


--
-- Name: il_instru_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX il_instru_fk_i ON clinlims.instrument_log USING btree (instru_id);


--
-- Name: il_inv_item_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX il_inv_item_fk_i ON clinlims.inventory_location USING btree (inv_item_id);


--
-- Name: instru_scrip_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX instru_scrip_fk_i ON clinlims.instrument USING btree (scrip_id);


--
-- Name: inv_recpt_invitem_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX inv_recpt_invitem_fk_i ON clinlims.inventory_receipt USING btree (invitem_id);


--
-- Name: invcomp_invitem_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX invcomp_invitem_fk_i ON clinlims.inventory_component USING btree (invitem_id);


--
-- Name: invcomp_matcomp_fk_ii; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX invcomp_matcomp_fk_ii ON clinlims.inventory_component USING btree (material_component_id);


--
-- Name: invitem_invname_uk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX invitem_invname_uk ON clinlims.inventory_item USING btree (name);


--
-- Name: invitem_uom_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX invitem_uom_fk_i ON clinlims.inventory_item USING btree (uom_id);


--
-- Name: invloc_storage_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX invloc_storage_fk_i ON clinlims.inventory_location USING btree (storage_id);


--
-- Name: invrec_org_fk_ii; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX invrec_org_fk_ii ON clinlims.inventory_receipt USING btree (org_id);


--
-- Name: label_script_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX label_script_fk_i ON clinlims.label USING btree (scriptlet_id);


--
-- Name: ma_analyte_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX ma_analyte_fk_i ON clinlims.method_analyte USING btree (analyte_id);


--
-- Name: ma_method_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX ma_method_fk_i ON clinlims.method_analyte USING btree (method_id);


--
-- Name: methres_method_fk_iii; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX methres_method_fk_iii ON clinlims.method_result USING btree (method_id);


--
-- Name: methres_scrip_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX methres_scrip_fk_i ON clinlims.method_result USING btree (scrip_id);


--
-- Name: mls_lab_tp_org_mlt_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX mls_lab_tp_org_mlt_fk_i ON clinlims.mls_lab_type USING btree (org_mlt_org_mlt_id);


--
-- Name: note_sys_user_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX note_sys_user_fk_i ON clinlims.note USING btree (sys_user_id);


--
-- Name: obs_history_sample_idx; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX obs_history_sample_idx ON clinlims.observation_history USING btree (sample_id);


--
-- Name: ord_org_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX ord_org_fk_i ON clinlims.orders USING btree (org_id);


--
-- Name: ord_sys_user_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX ord_sys_user_fk_i ON clinlims.orders USING btree (sys_user_id);


--
-- Name: orditem_inv_loc_fk_iii; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX orditem_inv_loc_fk_iii ON clinlims.order_item USING btree (inv_loc_id);


--
-- Name: orditem_ord_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX orditem_ord_fk_i ON clinlims.order_item USING btree (ord_id);


--
-- Name: org_org_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX org_org_fk_i ON clinlims.organization USING btree (org_id);


--
-- Name: org_org_mlt_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX org_org_mlt_fk_i ON clinlims.organization USING btree (org_mlt_org_mlt_id);


--
-- Name: pan_it_panel_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX pan_it_panel_fk_i ON clinlims.panel_item USING btree (panel_id);


--
-- Name: pat_person_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX pat_person_fk_i ON clinlims.patient USING btree (person_id);


--
-- Name: pr_pat_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX pr_pat_fk_i ON clinlims.patient_relations USING btree (pat_id);


--
-- Name: pr_pat_source_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX pr_pat_source_fk_i ON clinlims.patient_relations USING btree (pat_id_source);


--
-- Name: proj_sys_user_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX proj_sys_user_fk_i ON clinlims.project USING btree (sys_user_id);


--
-- Name: project_script_fk_iii; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX project_script_fk_iii ON clinlims.project USING btree (scriptlet_id);


--
-- Name: projectparam_proj_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX projectparam_proj_fk_i ON clinlims.project_parameter USING btree (project_id);


--
-- Name: provider_person_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX provider_person_fk_i ON clinlims.provider USING btree (person_id);


--
-- Name: qaev_test_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX qaev_test_fk_i ON clinlims.qa_event USING btree (test_id);


--
-- Name: qc_sys_user_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX qc_sys_user_fk_i ON clinlims.qc USING btree (sys_user_id);


--
-- Name: qc_uom_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX qc_uom_fk_i ON clinlims.qc USING btree (uom_id);


--
-- Name: qcanlyt_analyte_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX qcanlyt_analyte_fk_i ON clinlims.qc_analytes USING btree (analyte_id);


--
-- Name: referring_sample_item_indx; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX referring_sample_item_indx ON clinlims.referring_test_result USING btree (sample_item_id);


--
-- Name: report_import_date; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX report_import_date ON clinlims.report_external_import USING btree (event_date);


--
-- Name: report_queue_date; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX report_queue_date ON clinlims.report_external_export USING btree (event_date);


--
-- Name: result_analysis_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX result_analysis_fk_i ON clinlims.result USING btree (analysis_id);


--
-- Name: result_analyte_fk_iii; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX result_analyte_fk_iii ON clinlims.result USING btree (analyte_id);


--
-- Name: result_testresult_fk_1; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX result_testresult_fk_1 ON clinlims.result USING btree (test_result_id);


--
-- Name: samp_org_org_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX samp_org_org_fk_i ON clinlims.sample_organization USING btree (org_id);


--
-- Name: samp_org_samp_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX samp_org_samp_fk_i ON clinlims.sample_organization USING btree (samp_id);


--
-- Name: samp_package_1_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX samp_package_1_fk_i ON clinlims.sample USING btree (package_id);


--
-- Name: samp_sys_user_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX samp_sys_user_fk_i ON clinlims.sample USING btree (sys_user_id);


--
-- Name: sampitem_samp_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sampitem_samp_fk_i ON clinlims.sample_item USING btree (samp_id);


--
-- Name: sampitem_samp_item_uk_uk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX sampitem_samp_item_uk_uk ON clinlims.sample_item USING btree (id, sort_order);


--
-- Name: sampitem_sampitem_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sampitem_sampitem_fk_i ON clinlims.sample_item USING btree (sampitem_id);


--
-- Name: sampitem_source_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sampitem_source_fk_i ON clinlims.sample_item USING btree (source_id);


--
-- Name: sampitem_typeosamp_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sampitem_typeosamp_fk_i ON clinlims.sample_item USING btree (typeosamp_id);


--
-- Name: sampitem_uom_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sampitem_uom_fk_i ON clinlims.sample_item USING btree (uom_id);


--
-- Name: sample_projects_pk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX sample_projects_pk ON clinlims.sample_projects USING btree (id);


--
-- Name: sampledom_dom_uk_uk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX sampledom_dom_uk_uk ON clinlims.sample_domain USING btree (domain);


--
-- Name: source_dom_desc_uk_uk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX source_dom_desc_uk_uk ON clinlims.source_of_sample USING btree (description, domain);


--
-- Name: sp_proj_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sp_proj_fk_i ON clinlims.sample_projects USING btree (proj_id);


--
-- Name: sp_samp_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sp_samp_fk_i ON clinlims.sample_projects USING btree (samp_id);


--
-- Name: storloc_parent_storloc_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX storloc_parent_storloc_fk_i ON clinlims.storage_location USING btree (parent_storageloc_id);


--
-- Name: storloc_storunit_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX storloc_storunit_fk_i ON clinlims.storage_location USING btree (storage_unit_id);


--
-- Name: sysrolemodule_sysmodule_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sysrolemodule_sysmodule_fk_i ON clinlims.system_role_module USING btree (system_module_id);


--
-- Name: sysrolemodule_sysuser_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sysrolemodule_sysuser_fk_i ON clinlims.system_role_module USING btree (system_role_id);


--
-- Name: sysusermodule_sysmodule_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sysusermodule_sysmodule_fk_i ON clinlims.system_user_module USING btree (system_module_id);


--
-- Name: sysusermodule_sysuser_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sysusermodule_sysuser_fk_i ON clinlims.system_user_module USING btree (system_user_id);


--
-- Name: sysusersect_sysuser_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sysusersect_sysuser_fk_i ON clinlims.system_user_section USING btree (system_user_id);


--
-- Name: sysusersect_testsect_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX sysusersect_testsect_fk_i ON clinlims.system_user_section USING btree (test_section_id);


--
-- Name: test_desc_uk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX test_desc_uk ON clinlims.test USING btree (description);


--
-- Name: test_label_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_label_fk_i ON clinlims.test USING btree (label_id);


--
-- Name: test_method_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_method_fk_i ON clinlims.test USING btree (method_id);


--
-- Name: test_reflx_tst_rslt_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_reflx_tst_rslt_fk_i ON clinlims.test_reflex USING btree (tst_rslt_id);


--
-- Name: test_scriptlet_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_scriptlet_fk_i ON clinlims.test USING btree (scriptlet_id);


--
-- Name: test_sect_org_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_sect_org_fk_i ON clinlims.test_section USING btree (org_id);


--
-- Name: test_testformat_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_testformat_fk_i ON clinlims.test USING btree (test_format_id);


--
-- Name: test_testsect_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_testsect_fk_i ON clinlims.test USING btree (test_section_id);


--
-- Name: test_testtrailer_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_testtrailer_fk_i ON clinlims.test USING btree (test_trailer_id);


--
-- Name: test_uom_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX test_uom_fk_i ON clinlims.test USING btree (uom_id);


--
-- Name: testalyt_analyte_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX testalyt_analyte_fk_i ON clinlims.test_analyte USING btree (analyte_id);


--
-- Name: testalyt_test_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX testalyt_test_fk_i ON clinlims.test_analyte USING btree (test_id);


--
-- Name: testreflex_addtest_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX testreflex_addtest_fk_i ON clinlims.test_reflex USING btree (add_test_id);


--
-- Name: testreflex_test_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX testreflex_test_fk_i ON clinlims.test_reflex USING btree (test_id);


--
-- Name: testreflex_testanalyt_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX testreflex_testanalyt_fk_i ON clinlims.test_reflex USING btree (test_analyte_id);


--
-- Name: testresult_scriptlet_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX testresult_scriptlet_fk_i ON clinlims.test_result USING btree (scriptlet_id);


--
-- Name: tst_rslt_test_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX tst_rslt_test_fk_i ON clinlims.test_result USING btree (test_id);


--
-- Name: tw_test_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX tw_test_fk_i ON clinlims.test_worksheets USING btree (test_id);


--
-- Name: twi_qc_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX twi_qc_fk_i ON clinlims.test_worksheet_item USING btree (qc_id);


--
-- Name: twi_tw_fk_i; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE INDEX twi_tw_fk_i ON clinlims.test_worksheet_item USING btree (tw_id);


--
-- Name: typeosamp_dom_desc_uk_uk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX typeosamp_dom_desc_uk_uk ON clinlims.type_of_sample USING btree (description, domain);


--
-- Name: typofprov_desc_uk_uk; Type: INDEX; Schema: clinlims; Owner: clinlims
--

CREATE UNIQUE INDEX typofprov_desc_uk_uk ON clinlims.type_of_provider USING btree (description);


--
-- Name: analysis_panel_FK; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis
    ADD CONSTRAINT "analysis_panel_FK" FOREIGN KEY (panel_id) REFERENCES clinlims.panel(id) ON DELETE SET NULL;


--
-- Name: analysis_parent_analysis_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis
    ADD CONSTRAINT analysis_parent_analysis_fk FOREIGN KEY (parent_analysis_id) REFERENCES clinlims.analysis(id) MATCH FULL;


--
-- Name: analysis_parent_result_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis
    ADD CONSTRAINT analysis_parent_result_fk FOREIGN KEY (parent_result_id) REFERENCES clinlims.result(id) ON DELETE SET NULL;


--
-- Name: analysis_sampitem_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis
    ADD CONSTRAINT analysis_sampitem_fk FOREIGN KEY (sampitem_id) REFERENCES clinlims.sample_item(id) MATCH FULL;


--
-- Name: analysis_status_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis
    ADD CONSTRAINT analysis_status_fk FOREIGN KEY (status_id) REFERENCES clinlims.status_of_sample(id);


--
-- Name: analysis_test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis
    ADD CONSTRAINT analysis_test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id) MATCH FULL;


--
-- Name: analysis_test_sect_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis
    ADD CONSTRAINT analysis_test_sect_fk FOREIGN KEY (test_sect_id) REFERENCES clinlims.test_section(id) MATCH FULL;


--
-- Name: analyte_analyte_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyte
    ADD CONSTRAINT analyte_analyte_fk FOREIGN KEY (analyte_id) REFERENCES clinlims.analyte(id) MATCH FULL;


--
-- Name: analyzer_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyzer_results
    ADD CONSTRAINT analyzer_fk FOREIGN KEY (analyzer_id) REFERENCES clinlims.analyzer(id);


--
-- Name: analyzer_test_map_analyzer_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyzer_test_map
    ADD CONSTRAINT analyzer_test_map_analyzer_fk FOREIGN KEY (analyzer_id) REFERENCES clinlims.analyzer(id);


--
-- Name: analyzer_test_map_test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyzer_test_map
    ADD CONSTRAINT analyzer_test_map_test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id);


--
-- Name: anqaev_anal_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis_qaevent
    ADD CONSTRAINT anqaev_anal_fk FOREIGN KEY (analysis_id) REFERENCES clinlims.analysis(id) MATCH FULL;


--
-- Name: anqaev_qa_event_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analysis_qaevent
    ADD CONSTRAINT anqaev_qa_event_fk FOREIGN KEY (qa_event_id) REFERENCES clinlims.qa_event(id) MATCH FULL;


--
-- Name: attachmtitem_attachmt_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.attachment_item
    ADD CONSTRAINT attachmtitem_attachmt_fk FOREIGN KEY (attachment_id) REFERENCES clinlims.attachment(id) MATCH FULL;


--
-- Name: cd_elmt_xref_cd_elmt_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.code_element_xref
    ADD CONSTRAINT cd_elmt_xref_cd_elmt_type_fk FOREIGN KEY (code_element_type_id) REFERENCES clinlims.code_element_type(id) MATCH FULL;


--
-- Name: cd_elmt_xref_message_org_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.code_element_xref
    ADD CONSTRAINT cd_elmt_xref_message_org_fk FOREIGN KEY (message_org_id) REFERENCES clinlims.message_org(id) MATCH FULL;


--
-- Name: cd_elmt_xref_rcvr_cd_elmt_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.code_element_xref
    ADD CONSTRAINT cd_elmt_xref_rcvr_cd_elmt_fk FOREIGN KEY (receiver_code_element_id) REFERENCES clinlims.receiver_code_element(id) MATCH FULL;


--
-- Name: demographics_history_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.observation_history
    ADD CONSTRAINT demographics_history_type_fk FOREIGN KEY (observation_history_type_id) REFERENCES clinlims.observation_history_type(id);


--
-- Name: dictionary_dict_cat_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.dictionary
    ADD CONSTRAINT dictionary_dict_cat_fk FOREIGN KEY (dictionary_category_id) REFERENCES clinlims.dictionary_category(id) MATCH FULL;


--
-- Name: electronic_order_patient_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.electronic_order
    ADD CONSTRAINT electronic_order_patient_fk FOREIGN KEY (patient_id) REFERENCES clinlims.patient(id);


--
-- Name: env_samp_samp_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_environmental
    ADD CONSTRAINT env_samp_samp_fk FOREIGN KEY (samp_id) REFERENCES clinlims.sample(id) MATCH FULL;


--
-- Name: fk_doc_parent_id; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.document_track
    ADD CONSTRAINT fk_doc_parent_id FOREIGN KEY (parent_id) REFERENCES clinlims.document_track(id);


--
-- Name: fk_doc_type; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.document_track
    ADD CONSTRAINT fk_doc_type FOREIGN KEY (document_type_id) REFERENCES clinlims.document_type(id);


--
-- Name: fk_sample_type_localization; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.type_of_sample
    ADD CONSTRAINT fk_sample_type_localization FOREIGN KEY (name_localization_id) REFERENCES clinlims.localization(id);


--
-- Name: fk_sibling_reflex; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_reflex
    ADD CONSTRAINT fk_sibling_reflex FOREIGN KEY (sibling_reflex) REFERENCES clinlims.test_reflex(id) ON DELETE CASCADE;


--
-- Name: fk_table_id; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.document_track
    ADD CONSTRAINT fk_table_id FOREIGN KEY (table_id) REFERENCES clinlims.reference_tables(id);


--
-- Name: fk_test_dictioanry_test; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_dictionary
    ADD CONSTRAINT fk_test_dictioanry_test FOREIGN KEY (test_id) REFERENCES clinlims.test(id) ON DELETE CASCADE;


--
-- Name: fk_test_dictionary_dictionary; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_dictionary
    ADD CONSTRAINT fk_test_dictionary_dictionary FOREIGN KEY (dictionary_category_id) REFERENCES clinlims.dictionary_category(id) ON DELETE CASCADE;


--
-- Name: fk_test_dictionary_qualifiable; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_dictionary
    ADD CONSTRAINT fk_test_dictionary_qualifiable FOREIGN KEY (qualifiable_entry_id) REFERENCES clinlims.dictionary(id) ON DELETE CASCADE;


--
-- Name: history_sysuer_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.history
    ADD CONSTRAINT history_sysuer_fk FOREIGN KEY (sys_user_id) REFERENCES clinlims.system_user(id) MATCH FULL;


--
-- Name: ia_analyte_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.instrument_analyte
    ADD CONSTRAINT ia_analyte_fk FOREIGN KEY (analyte_id) REFERENCES clinlims.analyte(id) MATCH FULL;


--
-- Name: ia_instru_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.instrument_analyte
    ADD CONSTRAINT ia_instru_fk FOREIGN KEY (instru_id) REFERENCES clinlims.instrument(id) MATCH FULL;


--
-- Name: ia_method_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.instrument_analyte
    ADD CONSTRAINT ia_method_fk FOREIGN KEY (method_id) REFERENCES clinlims.method(id) MATCH FULL;


--
-- Name: identity_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_identity
    ADD CONSTRAINT identity_type_fk FOREIGN KEY (identity_type_id) REFERENCES clinlims.patient_identity_type(id);


--
-- Name: il_instru_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.instrument_log
    ADD CONSTRAINT il_instru_fk FOREIGN KEY (instru_id) REFERENCES clinlims.instrument(id) MATCH FULL;


--
-- Name: il_inv_item_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_location
    ADD CONSTRAINT il_inv_item_fk FOREIGN KEY (inv_item_id) REFERENCES clinlims.inventory_item(id) MATCH FULL;


--
-- Name: inv_recpt_invitem_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_receipt
    ADD CONSTRAINT inv_recpt_invitem_fk FOREIGN KEY (invitem_id) REFERENCES clinlims.inventory_item(id) MATCH FULL;


--
-- Name: invcomp_invitem_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_component
    ADD CONSTRAINT invcomp_invitem_fk FOREIGN KEY (invitem_id) REFERENCES clinlims.inventory_item(id) MATCH FULL;


--
-- Name: invcomp_matcomp_fk_iii; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_component
    ADD CONSTRAINT invcomp_matcomp_fk_iii FOREIGN KEY (material_component_id) REFERENCES clinlims.inventory_item(id) MATCH FULL;


--
-- Name: inventory__location_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_inventory
    ADD CONSTRAINT inventory__location_fk FOREIGN KEY (inventory_location_id) REFERENCES clinlims.inventory_location(id);


--
-- Name: invitem_uom_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_item
    ADD CONSTRAINT invitem_uom_fk FOREIGN KEY (uom_id) REFERENCES clinlims.unit_of_measure(id) MATCH FULL;


--
-- Name: invloc_storage_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_location
    ADD CONSTRAINT invloc_storage_fk FOREIGN KEY (storage_id) REFERENCES clinlims.storage_location(id) MATCH FULL;


--
-- Name: invrec_org_fk_iii; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.inventory_receipt
    ADD CONSTRAINT invrec_org_fk_iii FOREIGN KEY (org_id) REFERENCES clinlims.organization(id) MATCH FULL;


--
-- Name: label_script_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.label
    ADD CONSTRAINT label_script_fk FOREIGN KEY (scriptlet_id) REFERENCES clinlims.scriptlet(id) MATCH FULL;


--
-- Name: ma_analyte_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.method_analyte
    ADD CONSTRAINT ma_analyte_fk FOREIGN KEY (analyte_id) REFERENCES clinlims.analyte(id) MATCH FULL;


--
-- Name: ma_method_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.method_analyte
    ADD CONSTRAINT ma_method_fk FOREIGN KEY (method_id) REFERENCES clinlims.method(id) MATCH FULL;


--
-- Name: menu_parent_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.menu
    ADD CONSTRAINT menu_parent_fk FOREIGN KEY (parent_id) REFERENCES clinlims.menu(id);


--
-- Name: methres_method_fk_ii; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.method_result
    ADD CONSTRAINT methres_method_fk_ii FOREIGN KEY (method_id) REFERENCES clinlims.method(id) MATCH FULL;


--
-- Name: methres_scrip_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.method_result
    ADD CONSTRAINT methres_scrip_fk FOREIGN KEY (scrip_id) REFERENCES clinlims.scriptlet(id) MATCH FULL;


--
-- Name: mls_lab_tp_org_mlt_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.mls_lab_type
    ADD CONSTRAINT mls_lab_tp_org_mlt_fk FOREIGN KEY (org_mlt_org_mlt_id) REFERENCES clinlims.org_mls_lab_type(org_mlt_id) MATCH FULL;


--
-- Name: name_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT name_fk FOREIGN KEY (name_localization_id) REFERENCES clinlims.localization(id) ON DELETE CASCADE;


--
-- Name: name_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_section
    ADD CONSTRAINT name_fk FOREIGN KEY (name_localization_id) REFERENCES clinlims.localization(id) ON DELETE CASCADE;


--
-- Name: name_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.panel
    ADD CONSTRAINT name_fk FOREIGN KEY (name_localization_id) REFERENCES clinlims.localization(id) ON DELETE CASCADE;


--
-- Name: nce_action_log_nc_event_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nce_action_log
    ADD CONSTRAINT nce_action_log_nc_event_fk FOREIGN KEY (nce_event_id) REFERENCES clinlims.nc_event(id);


--
-- Name: nce_category_nc_event_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nc_event
    ADD CONSTRAINT nce_category_nc_event_fk FOREIGN KEY (nce_category_id) REFERENCES clinlims.nce_category(id);


--
-- Name: nce_category_nce_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nce_type
    ADD CONSTRAINT nce_category_nce_type_fk FOREIGN KEY (category_id) REFERENCES clinlims.nce_category(id);


--
-- Name: nce_type_nc_event_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nc_event
    ADD CONSTRAINT nce_type_nc_event_fk FOREIGN KEY (nce_type_id) REFERENCES clinlims.nce_type(id);


--
-- Name: note_sys_user_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.note
    ADD CONSTRAINT note_sys_user_fk FOREIGN KEY (sys_user_id) REFERENCES clinlims.system_user(id) MATCH FULL;


--
-- Name: ord_org_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.orders
    ADD CONSTRAINT ord_org_fk FOREIGN KEY (org_id) REFERENCES clinlims.organization(id) MATCH FULL;


--
-- Name: ord_sys_user_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.orders
    ADD CONSTRAINT ord_sys_user_fk FOREIGN KEY (sys_user_id) REFERENCES clinlims.system_user(id) MATCH FULL;


--
-- Name: orditem_inv_loc_fk_ii; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.order_item
    ADD CONSTRAINT orditem_inv_loc_fk_ii FOREIGN KEY (inv_loc_id) REFERENCES clinlims.inventory_location(id) MATCH FULL;


--
-- Name: orditem_ord_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.order_item
    ADD CONSTRAINT orditem_ord_fk FOREIGN KEY (ord_id) REFERENCES clinlims.orders(id) MATCH FULL;


--
-- Name: org_contact_org_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_contact
    ADD CONSTRAINT org_contact_org_fk FOREIGN KEY (organization_id) REFERENCES clinlims.organization(id);


--
-- Name: org_contact_person_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_contact
    ADD CONSTRAINT org_contact_person_fk FOREIGN KEY (person_id) REFERENCES clinlims.person(id);


--
-- Name: org_hl7_encoding_id_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.org_hl7_encoding_type
    ADD CONSTRAINT org_hl7_encoding_id_fk FOREIGN KEY (encoding_type_id) REFERENCES clinlims.test_code_type(id);


--
-- Name: org_hl7_org_id_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.org_hl7_encoding_type
    ADD CONSTRAINT org_hl7_org_id_fk FOREIGN KEY (organization_id) REFERENCES clinlims.organization(id);


--
-- Name: org_org_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization
    ADD CONSTRAINT org_org_fk FOREIGN KEY (org_id) REFERENCES clinlims.organization(id) MATCH FULL;


--
-- Name: org_org_mlt_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization
    ADD CONSTRAINT org_org_mlt_fk FOREIGN KEY (org_mlt_org_mlt_id) REFERENCES clinlims.org_mls_lab_type(org_mlt_id) MATCH FULL;


--
-- Name: organization_address_address_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_address
    ADD CONSTRAINT organization_address_address_fk FOREIGN KEY (address_part_id) REFERENCES clinlims.address_part(id);


--
-- Name: organization_address_organization_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_address
    ADD CONSTRAINT organization_address_organization_fk FOREIGN KEY (organization_id) REFERENCES clinlims.organization(id);


--
-- Name: organization_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.project_organization
    ADD CONSTRAINT organization_fk FOREIGN KEY (org_id) REFERENCES clinlims.organization(id);


--
-- Name: organization_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_organization_type
    ADD CONSTRAINT organization_fk FOREIGN KEY (org_id) REFERENCES clinlims.organization(id) ON DELETE CASCADE;


--
-- Name: organization_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.organization_organization_type
    ADD CONSTRAINT organization_type_fk FOREIGN KEY (org_type_id) REFERENCES clinlims.organization_type(id);


--
-- Name: pan_it_panel_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.panel_item
    ADD CONSTRAINT pan_it_panel_fk FOREIGN KEY (panel_id) REFERENCES clinlims.panel(id) MATCH FULL;


--
-- Name: param_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_module_url
    ADD CONSTRAINT param_fk FOREIGN KEY (system_module_param_id) REFERENCES clinlims.system_module_param(id);


--
-- Name: parent_test_sec_test_sect_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_section
    ADD CONSTRAINT parent_test_sec_test_sect_fk FOREIGN KEY (parent_test_section) REFERENCES clinlims.test_section(id) MATCH FULL;


--
-- Name: pat_person_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient
    ADD CONSTRAINT pat_person_fk FOREIGN KEY (person_id) REFERENCES clinlims.person(id) MATCH FULL;


--
-- Name: patient_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_identity
    ADD CONSTRAINT patient_fk FOREIGN KEY (patient_id) REFERENCES clinlims.patient(id);


--
-- Name: patient_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_patient_type
    ADD CONSTRAINT patient_fk FOREIGN KEY (patient_id) REFERENCES clinlims.patient(id);


--
-- Name: patient_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.observation_history
    ADD CONSTRAINT patient_fk FOREIGN KEY (patient_id) REFERENCES clinlims.patient(id);


--
-- Name: patient_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_patient_type
    ADD CONSTRAINT patient_type_fk FOREIGN KEY (patient_type_id) REFERENCES clinlims.patient_type(id);


--
-- Name: person_address_address_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.person_address
    ADD CONSTRAINT person_address_address_fk FOREIGN KEY (address_part_id) REFERENCES clinlims.address_part(id);


--
-- Name: person_address_person_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.person_address
    ADD CONSTRAINT person_address_person_fk FOREIGN KEY (person_id) REFERENCES clinlims.person(id);


--
-- Name: pr_pat_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_relations
    ADD CONSTRAINT pr_pat_fk FOREIGN KEY (pat_id) REFERENCES clinlims.patient(id) MATCH FULL;


--
-- Name: pr_pat_source_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.patient_relations
    ADD CONSTRAINT pr_pat_source_fk FOREIGN KEY (pat_id_source) REFERENCES clinlims.patient(id) MATCH FULL;


--
-- Name: project_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.project_organization
    ADD CONSTRAINT project_fk FOREIGN KEY (project_id) REFERENCES clinlims.project(id);


--
-- Name: project_script_fk_ii; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.project
    ADD CONSTRAINT project_script_fk_ii FOREIGN KEY (scriptlet_id) REFERENCES clinlims.scriptlet(id) MATCH FULL;


--
-- Name: project_sysuer_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.project
    ADD CONSTRAINT project_sysuer_fk FOREIGN KEY (sys_user_id) REFERENCES clinlims.system_user(id) MATCH FULL;


--
-- Name: projectparam_proj_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.project_parameter
    ADD CONSTRAINT projectparam_proj_fk FOREIGN KEY (project_id) REFERENCES clinlims.project(id) MATCH FULL;


--
-- Name: prov_person_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.provider
    ADD CONSTRAINT prov_person_fk FOREIGN KEY (person_id) REFERENCES clinlims.person(id) MATCH FULL;


--
-- Name: qa_observation_type_k; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qa_observation
    ADD CONSTRAINT qa_observation_type_k FOREIGN KEY (qa_observation_type_id) REFERENCES clinlims.qa_observation_type(id);


--
-- Name: qaev_test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qa_event
    ADD CONSTRAINT qaev_test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id) MATCH FULL;


--
-- Name: qc_sys_user_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qc
    ADD CONSTRAINT qc_sys_user_fk FOREIGN KEY (sys_user_id) REFERENCES clinlims.system_user(id) MATCH FULL;


--
-- Name: qc_uom_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qc
    ADD CONSTRAINT qc_uom_fk FOREIGN KEY (uom_id) REFERENCES clinlims.unit_of_measure(id) MATCH FULL;


--
-- Name: qcanlyt_analyte_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qc_analytes
    ADD CONSTRAINT qcanlyt_analyte_fk FOREIGN KEY (analyte_id) REFERENCES clinlims.analyte(id) MATCH FULL;


--
-- Name: qrtz_blob_triggers_trigger_name_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_trigger_name_fkey FOREIGN KEY (trigger_name, trigger_group) REFERENCES clinlims.qrtz_triggers(trigger_name, trigger_group);


--
-- Name: qrtz_cron_triggers_trigger_name_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_trigger_name_fkey FOREIGN KEY (trigger_name, trigger_group) REFERENCES clinlims.qrtz_triggers(trigger_name, trigger_group);


--
-- Name: qrtz_job_listeners_job_name_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_job_listeners
    ADD CONSTRAINT qrtz_job_listeners_job_name_fkey FOREIGN KEY (job_name, job_group) REFERENCES clinlims.qrtz_job_details(job_name, job_group);


--
-- Name: qrtz_simple_triggers_trigger_name_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_trigger_name_fkey FOREIGN KEY (trigger_name, trigger_group) REFERENCES clinlims.qrtz_triggers(trigger_name, trigger_group);


--
-- Name: qrtz_trigger_listeners_trigger_name_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_trigger_listeners
    ADD CONSTRAINT qrtz_trigger_listeners_trigger_name_fkey FOREIGN KEY (trigger_name, trigger_group) REFERENCES clinlims.qrtz_triggers(trigger_name, trigger_group);


--
-- Name: qrtz_triggers_job_name_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_job_name_fkey FOREIGN KEY (job_name, job_group) REFERENCES clinlims.qrtz_job_details(job_name, job_group);


--
-- Name: receiver_code_code_element_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.receiver_code_element
    ADD CONSTRAINT receiver_code_code_element_fk FOREIGN KEY (code_element_type_id) REFERENCES clinlims.code_element_type(id) MATCH FULL;


--
-- Name: receiver_code_message_org_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.receiver_code_element
    ADD CONSTRAINT receiver_code_message_org_fk FOREIGN KEY (message_org_id) REFERENCES clinlims.message_org(id) MATCH FULL;


--
-- Name: referral_analysis_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral
    ADD CONSTRAINT referral_analysis_fk FOREIGN KEY (analysis_id) REFERENCES clinlims.analysis(id);


--
-- Name: referral_organization_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral
    ADD CONSTRAINT referral_organization_fk FOREIGN KEY (organization_id) REFERENCES clinlims.organization(id) ON DELETE CASCADE;


--
-- Name: referral_reason_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral
    ADD CONSTRAINT referral_reason_fk FOREIGN KEY (referral_reason_id) REFERENCES clinlims.referral_reason(id);


--
-- Name: referral_result_referral_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral_result
    ADD CONSTRAINT referral_result_referral_fk FOREIGN KEY (referral_id) REFERENCES clinlims.referral(id) ON DELETE CASCADE;


--
-- Name: referral_result_result; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral_result
    ADD CONSTRAINT referral_result_result FOREIGN KEY (result_id) REFERENCES clinlims.result(id);


--
-- Name: referral_result_test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral_result
    ADD CONSTRAINT referral_result_test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id);


--
-- Name: referral_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referral
    ADD CONSTRAINT referral_type_fk FOREIGN KEY (referral_type_id) REFERENCES clinlims.referral_type(id);


--
-- Name: referring_sample_item_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.referring_test_result
    ADD CONSTRAINT referring_sample_item_fk FOREIGN KEY (sample_item_id) REFERENCES clinlims.sample_item(id) ON DELETE CASCADE;


--
-- Name: report_queue_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.report_external_export
    ADD CONSTRAINT report_queue_type_fk FOREIGN KEY (type) REFERENCES clinlims.report_queue_type(id);


--
-- Name: reporting_name_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT reporting_name_fk FOREIGN KEY (reporting_name_localization_id) REFERENCES clinlims.localization(id) ON DELETE CASCADE;


--
-- Name: reporting_unit_nc_event_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nc_event
    ADD CONSTRAINT reporting_unit_nc_event_fk FOREIGN KEY (reporting_unit_id) REFERENCES clinlims.test_section(id);


--
-- Name: requester_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_requester
    ADD CONSTRAINT requester_type_fk FOREIGN KEY (requester_type_id) REFERENCES clinlims.requester_type(id);


--
-- Name: result_analysis_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result
    ADD CONSTRAINT result_analysis_fk FOREIGN KEY (analysis_id) REFERENCES clinlims.analysis(id) MATCH FULL;


--
-- Name: result_analyte_fk_ii; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result
    ADD CONSTRAINT result_analyte_fk_ii FOREIGN KEY (analyte_id) REFERENCES clinlims.analyte(id) MATCH FULL;


--
-- Name: result_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_inventory
    ADD CONSTRAINT result_fk FOREIGN KEY (result_id) REFERENCES clinlims.result(id);


--
-- Name: result_id_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_signature
    ADD CONSTRAINT result_id_fk FOREIGN KEY (result_id) REFERENCES clinlims.result(id);


--
-- Name: result_parent_id_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result
    ADD CONSTRAINT result_parent_id_fk FOREIGN KEY (parent_id) REFERENCES clinlims.result(id) ON DELETE CASCADE;


--
-- Name: result_testresult_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result
    ADD CONSTRAINT result_testresult_fk FOREIGN KEY (test_result_id) REFERENCES clinlims.test_result(id) MATCH FULL;


--
-- Name: role_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_role
    ADD CONSTRAINT role_fk FOREIGN KEY (role_id) REFERENCES clinlims.system_role(id) ON DELETE CASCADE;


--
-- Name: role_parent_role_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_role
    ADD CONSTRAINT role_parent_role_fk FOREIGN KEY (grouping_parent) REFERENCES clinlims.system_role(id);


--
-- Name: samp_org_org_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_organization
    ADD CONSTRAINT samp_org_org_fk FOREIGN KEY (org_id) REFERENCES clinlims.organization(id) MATCH FULL;


--
-- Name: samp_org_samp_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_organization
    ADD CONSTRAINT samp_org_samp_fk FOREIGN KEY (samp_id) REFERENCES clinlims.sample(id) MATCH FULL;


--
-- Name: samphuman_patient_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_human
    ADD CONSTRAINT samphuman_patient_fk FOREIGN KEY (patient_id) REFERENCES clinlims.patient(id) MATCH FULL;


--
-- Name: samphuman_provider_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_human
    ADD CONSTRAINT samphuman_provider_fk FOREIGN KEY (provider_id) REFERENCES clinlims.provider(id) MATCH FULL;


--
-- Name: samphuman_sample_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_human
    ADD CONSTRAINT samphuman_sample_fk FOREIGN KEY (samp_id) REFERENCES clinlims.sample(id) MATCH FULL;


--
-- Name: sampitem_sampitem_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_item
    ADD CONSTRAINT sampitem_sampitem_fk FOREIGN KEY (sampitem_id) REFERENCES clinlims.sample_item(id) MATCH FULL;


--
-- Name: sampitem_sample_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_item
    ADD CONSTRAINT sampitem_sample_fk FOREIGN KEY (samp_id) REFERENCES clinlims.sample(id) MATCH FULL;


--
-- Name: sampitem_sourceosamp_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_item
    ADD CONSTRAINT sampitem_sourceosamp_fk FOREIGN KEY (source_id) REFERENCES clinlims.source_of_sample(id) MATCH FULL;


--
-- Name: sampitem_typeosamp_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_item
    ADD CONSTRAINT sampitem_typeosamp_fk FOREIGN KEY (typeosamp_id) REFERENCES clinlims.type_of_sample(id) MATCH FULL;


--
-- Name: sampitem_uom_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_item
    ADD CONSTRAINT sampitem_uom_fk FOREIGN KEY (uom_id) REFERENCES clinlims.unit_of_measure(id) MATCH FULL;


--
-- Name: sample_electronic_order_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample
    ADD CONSTRAINT sample_electronic_order_fk FOREIGN KEY (clinical_order_id) REFERENCES clinlims.electronic_order(id);


--
-- Name: sample_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.observation_history
    ADD CONSTRAINT sample_fk FOREIGN KEY (sample_id) REFERENCES clinlims.sample(id);


--
-- Name: sample_item_specimen_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nce_specimen
    ADD CONSTRAINT sample_item_specimen_fk FOREIGN KEY (sample_item_id) REFERENCES clinlims.sample_item(id);


--
-- Name: sample_package_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample
    ADD CONSTRAINT sample_package_fk FOREIGN KEY (package_id) REFERENCES clinlims.package_1(id) MATCH FULL;


--
-- Name: sample_qaevent_sampleitem_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_qaevent
    ADD CONSTRAINT sample_qaevent_sampleitem_fk FOREIGN KEY (sampleitem_id) REFERENCES clinlims.sample_item(id);


--
-- Name: sample_status_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample
    ADD CONSTRAINT sample_status_fk FOREIGN KEY (status_id) REFERENCES clinlims.status_of_sample(id);


--
-- Name: sample_sysuser_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample
    ADD CONSTRAINT sample_sysuser_fk FOREIGN KEY (sys_user_id) REFERENCES clinlims.system_user(id) MATCH FULL;


--
-- Name: sampletype_panel_panel_id_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sampletype_panel
    ADD CONSTRAINT sampletype_panel_panel_id_fkey FOREIGN KEY (panel_id) REFERENCES clinlims.panel(id);


--
-- Name: sampletype_panel_sample_type_id_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sampletype_panel
    ADD CONSTRAINT sampletype_panel_sample_type_id_fkey FOREIGN KEY (sample_type_id) REFERENCES clinlims.type_of_sample(id);


--
-- Name: sampletype_test_sample_type_id_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sampletype_test
    ADD CONSTRAINT sampletype_test_sample_type_id_fkey FOREIGN KEY (sample_type_id) REFERENCES clinlims.type_of_sample(id);


--
-- Name: sampletype_test_test_id_fkey; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sampletype_test
    ADD CONSTRAINT sampletype_test_test_id_fkey FOREIGN KEY (test_id) REFERENCES clinlims.test(id);


--
-- Name: sampnewborn_sample_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_newborn
    ADD CONSTRAINT sampnewborn_sample_fk FOREIGN KEY (id) REFERENCES clinlims.sample_human(id);


--
-- Name: sampproj_project_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_projects
    ADD CONSTRAINT sampproj_project_fk FOREIGN KEY (proj_id) REFERENCES clinlims.project(id) MATCH FULL;


--
-- Name: sampproj_sample_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.sample_projects
    ADD CONSTRAINT sampproj_sample_fk FOREIGN KEY (samp_id) REFERENCES clinlims.sample(id) MATCH FULL;


--
-- Name: specimen_nc_event_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.nce_specimen
    ADD CONSTRAINT specimen_nc_event_fk FOREIGN KEY (nce_id) REFERENCES clinlims.nc_event(id);


--
-- Name: status_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.analyzer_results
    ADD CONSTRAINT status_fk FOREIGN KEY (status_id) REFERENCES clinlims.analyzer_result_status(id);


--
-- Name: status_id; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.electronic_order
    ADD CONSTRAINT status_id FOREIGN KEY (status_id) REFERENCES clinlims.status_of_sample(id);


--
-- Name: storloc_parent_storloc_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.storage_location
    ADD CONSTRAINT storloc_parent_storloc_fk FOREIGN KEY (parent_storageloc_id) REFERENCES clinlims.storage_location(id) MATCH FULL;


--
-- Name: storloc_storunit_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.storage_location
    ADD CONSTRAINT storloc_storunit_fk FOREIGN KEY (storage_unit_id) REFERENCES clinlims.storage_unit(id) MATCH FULL;


--
-- Name: sysrolemodule_sysmodule_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_role_module
    ADD CONSTRAINT sysrolemodule_sysmodule_fk FOREIGN KEY (system_module_id) REFERENCES clinlims.system_module(id) MATCH FULL ON DELETE CASCADE;


--
-- Name: sysrolemodule_sysrole_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_role_module
    ADD CONSTRAINT sysrolemodule_sysrole_fk FOREIGN KEY (system_role_id) REFERENCES clinlims.system_role(id) MATCH FULL ON DELETE CASCADE;


--
-- Name: system_module_id_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_module_url
    ADD CONSTRAINT system_module_id_fk FOREIGN KEY (system_module_id) REFERENCES clinlims.system_module(id);


--
-- Name: system_user_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_role
    ADD CONSTRAINT system_user_fk FOREIGN KEY (system_user_id) REFERENCES clinlims.system_user(id) ON DELETE CASCADE;


--
-- Name: system_user_id_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_signature
    ADD CONSTRAINT system_user_id_fk FOREIGN KEY (system_user_id) REFERENCES clinlims.system_user(id);


--
-- Name: sysusermodule_sysmodule_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_module
    ADD CONSTRAINT sysusermodule_sysmodule_fk FOREIGN KEY (system_module_id) REFERENCES clinlims.system_module(id) MATCH FULL;


--
-- Name: sysusermodule_sysuser_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_module
    ADD CONSTRAINT sysusermodule_sysuser_fk FOREIGN KEY (system_user_id) REFERENCES clinlims.system_user(id) MATCH FULL;


--
-- Name: sysusersect_sysuser_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_section
    ADD CONSTRAINT sysusersect_sysuser_fk FOREIGN KEY (system_user_id) REFERENCES clinlims.system_user(id) MATCH FULL;


--
-- Name: sysusersect_testsect_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.system_user_section
    ADD CONSTRAINT sysusersect_testsect_fk FOREIGN KEY (test_section_id) REFERENCES clinlims.test_section(id) MATCH FULL;


--
-- Name: test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_limits
    ADD CONSTRAINT test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id);


--
-- Name: test_hl7_encoding_id_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_code
    ADD CONSTRAINT test_hl7_encoding_id_fk FOREIGN KEY (code_type_id) REFERENCES clinlims.test_code_type(id);


--
-- Name: test_hl7_test_id_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_code
    ADD CONSTRAINT test_hl7_test_id_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id);


--
-- Name: test_label_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT test_label_fk FOREIGN KEY (label_id) REFERENCES clinlims.label(id) MATCH FULL;


--
-- Name: test_method_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT test_method_fk FOREIGN KEY (method_id) REFERENCES clinlims.method(id) MATCH FULL;


--
-- Name: test_reflex_scriptlet_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_reflex
    ADD CONSTRAINT test_reflex_scriptlet_fk FOREIGN KEY (scriptlet_id) REFERENCES clinlims.scriptlet(id);


--
-- Name: test_result_type_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.result_limits
    ADD CONSTRAINT test_result_type_fk FOREIGN KEY (test_result_type_id) REFERENCES clinlims.type_of_test_result(id);


--
-- Name: test_scriptlet_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT test_scriptlet_fk FOREIGN KEY (scriptlet_id) REFERENCES clinlims.scriptlet(id) MATCH FULL;


--
-- Name: test_sect_org_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_section
    ADD CONSTRAINT test_sect_org_fk FOREIGN KEY (org_id) REFERENCES clinlims.organization(id) MATCH FULL;


--
-- Name: test_testformat_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT test_testformat_fk FOREIGN KEY (test_format_id) REFERENCES clinlims.test_formats(id) MATCH FULL;


--
-- Name: test_testsect_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT test_testsect_fk FOREIGN KEY (test_section_id) REFERENCES clinlims.test_section(id) MATCH FULL;


--
-- Name: test_testtrailer_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT test_testtrailer_fk FOREIGN KEY (test_trailer_id) REFERENCES clinlims.test_trailer(id) MATCH FULL;


--
-- Name: test_uom_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test
    ADD CONSTRAINT test_uom_fk FOREIGN KEY (uom_id) REFERENCES clinlims.unit_of_measure(id) MATCH FULL;


--
-- Name: testalyt_analyte_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_analyte
    ADD CONSTRAINT testalyt_analyte_fk FOREIGN KEY (analyte_id) REFERENCES clinlims.analyte(id) MATCH FULL;


--
-- Name: testalyt_test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_analyte
    ADD CONSTRAINT testalyt_test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id) MATCH FULL;


--
-- Name: testreflex_addtest_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_reflex
    ADD CONSTRAINT testreflex_addtest_fk FOREIGN KEY (add_test_id) REFERENCES clinlims.test(id) MATCH FULL;


--
-- Name: testreflex_test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_reflex
    ADD CONSTRAINT testreflex_test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id) MATCH FULL;


--
-- Name: testreflex_testanalyt_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_reflex
    ADD CONSTRAINT testreflex_testanalyt_fk FOREIGN KEY (test_analyte_id) REFERENCES clinlims.test_analyte(id) MATCH FULL;


--
-- Name: testreflex_tstrslt_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_reflex
    ADD CONSTRAINT testreflex_tstrslt_fk FOREIGN KEY (tst_rslt_id) REFERENCES clinlims.test_result(id) MATCH FULL;


--
-- Name: testresult_scriptlet_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_result
    ADD CONSTRAINT testresult_scriptlet_fk FOREIGN KEY (scriptlet_id) REFERENCES clinlims.scriptlet(id) MATCH FULL;


--
-- Name: testresult_test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_result
    ADD CONSTRAINT testresult_test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id) MATCH FULL;


--
-- Name: tw_test_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_worksheets
    ADD CONSTRAINT tw_test_fk FOREIGN KEY (test_id) REFERENCES clinlims.test(id) MATCH FULL;


--
-- Name: twi_qc_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_worksheet_item
    ADD CONSTRAINT twi_qc_fk FOREIGN KEY (qc_id) REFERENCES clinlims.qc(id) MATCH FULL;


--
-- Name: twi_tw_fk; Type: FK CONSTRAINT; Schema: clinlims; Owner: clinlims
--

ALTER TABLE ONLY clinlims.test_worksheet_item
    ADD CONSTRAINT twi_tw_fk FOREIGN KEY (tw_id) REFERENCES clinlims.test_worksheets(id) MATCH FULL;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

