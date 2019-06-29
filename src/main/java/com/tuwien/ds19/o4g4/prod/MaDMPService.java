package com.tuwien.ds19.o4g4.prod;

import com.google.gson.Gson;
import com.tuwien.ds19.o4g4.prod.config.HorizonTemplateConfig;
import com.tuwien.ds19.o4g4.prod.data.AnswersRepository;
import com.tuwien.ds19.o4g4.prod.data.DMPJSON;
import com.tuwien.ds19.o4g4.prod.data.entity.Answer;
import com.tuwien.ds19.o4g4.prod.data.entity.madmp.*;
import com.tuwien.ds19.o4g4.prod.data.entity.Plan;
import com.tuwien.ds19.o4g4.prod.util.DataAccessType;
import com.tuwien.ds19.o4g4.prod.util.TFAnswer;
import com.tuwien.ds19.o4g4.prod.util.TypeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MaDMPService {

    private final AnswersRepository answersRepository;

    private Logger logger = LoggerFactory.getLogger(MaDMPService.class);

    public MaDMPService(AnswersRepository answersRepository) {
        this.answersRepository = answersRepository;
    }

    public DMP getDMPFromPlan(Plan p) {

        DMP newDmp = new DMP();
        newDmp.setTitle(p.getTitle());
        newDmp.setDescription(p.getDescription());
        newDmp.setLanguage("en");
        newDmp.setCreated(p.getCreated());
        newDmp.setModified(p.getUpdated());
        newDmp.setDataset(new ArrayList<>());
        newDmp.getDataset().add(new Dataset(new Distribution()));

        setContact(p, newDmp);
        setProject(p, newDmp);

        parseAnswers(p, newDmp);

        return newDmp;
    }

    public String getJSONDMP(DMP dmp) {
        Gson gson = new Gson();
        return gson.toJson(new DMPJSON(dmp));
    }

    private void parseAnswers(Plan p, DMP dmp) {
        List<Answer> answers = answersRepository.findAllByPlanId(p.getId());

        // check if this plan is from Horizon 2020 or FWF template
        int q_id = answers.get(0).getQuestion_id();

        if (q_id >= HorizonTemplateConfig.QUESTION_ID_RANGE_START &&
                q_id <= HorizonTemplateConfig.QUESTION_ID_RANGE_END) { // H2020
            Dataset ds = dmp.getDataset().get(0);
            Distribution dst = ds.getDistribution().get(0);
            for (Answer a : answers) {
                parseH2020Answer(a, dmp, ds, dst);
            }
        } else if (false) { // TODO FWF

        }
    }

    private void parseH2020Answer(Answer a, DMP dmp, Dataset ds, Distribution dist) {
        String text = a.getText();

        if(text.isEmpty()){
            return;
        }

        switch (HorizonTemplateConfig.QUESTIONS_MAP.get(a.getQuestion_id())) {
            case 1:
                dmp.getProject().get(0).setDescription(text);
                break;
            case 2:
                for (Dataset.DatasetTypeEnum dt : Dataset.DatasetTypeEnum.values()) {
                    if (text.toLowerCase().contains(dt.name())) {
                        ds.setType(dt.name());
                    }
                }
                for (Distribution.DatasetFormat df : Distribution.DatasetFormat.values()) {
                    if (text.toLowerCase().contains(df.name())) {
                        dist.setFormat(df.name());
                    }
                }
                break;
            case 3:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("reuse_existing_data")));
                break;
            case 4:
                Pattern p = Pattern.compile("((https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])");
                Matcher m = p.matcher(text);
                if(m.matches()) {
                    dist.setAccessURL(m.group(1));
                    dist.setDownloadURL(m.group(1));
                }
                break;
            case 5:
                int size = Integer.parseInt(text.replaceAll("[^0-9]", ""));
                if (text.toLowerCase().contains("kb")) {
                    size = size * 1024;
                } else if (text.toLowerCase().contains("mb")) {
                    size = size * 1024 * 1024;
                } else if (text.toLowerCase().contains("gb")) {
                    size = size * 1024 * 1024 * 1024;
                }
                dist.setByteSize(size);
                break;
            case 6:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("useful_to")));
                break;
            case 7:
                Pattern pattern = Pattern.compile("(10.\\d{4,9}.[\\w.-]+)");
                Matcher matcher = pattern.matcher(text);
                Dataset_Id dataset_id = new Dataset_Id(matcher.find()? matcher.group(1) : text);
                dataset_id.setdataset_id_type(TypeIdentifier.checkType(text));
                ds.setDataset_id(dataset_id);
                break;
            case 8:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("naming_convention")));
                break;
            case 9:
                if(text.contains(",")) {
                    ds.setKeyword(Arrays.asList(text.split(",")));
                } else if(text.contains(";")){
                    ds.setKeyword(Arrays.asList(text.split(";")));
                } else {
                    ArrayList<String> list = new ArrayList<>();
                    list.add(text);
                    ds.setKeyword(list);
                }
                break;
            case 10:
                if(text.contains(TFAnswer.yes.name())){
                    dist.getHost().setSupports_versioning(TFAnswer.yes.name());
                } else if (text.contains(TFAnswer.no.name())){
                    dist.getHost().setSupports_versioning(TFAnswer.no.name());
                } else {
                    dist.getHost().setSupports_versioning(TFAnswer.unknown.name());
                }
                break;
            case 11:
            case 24:
            case 25:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("metadata_standard")));
                break;
            case 12:
                if(text.toLowerCase().contains(DataAccessType.open.name())){
                    dist.setData_access(DataAccessType.open.name());
                } else if (text.toLowerCase().contains(DataAccessType.closed.name())){
                    dist.setData_access(DataAccessType.closed.name());
                } else if (text.toLowerCase().contains(DataAccessType.shared.name())){
                    dist.setData_access(DataAccessType.shared.name());
                }
                if(text.toLowerCase().contains("personal")){
                    ds.setPersonalData(TFAnswer.yes.name());
                } else {
                    ds.setPersonalData(TFAnswer.unknown.name());
                    if(text.toLowerCase().contains("sensitive")){
                        ds.setSensitiveData(TFAnswer.yes.name());
                    } else {
                        ds.setSensitiveData(TFAnswer.unknown.name());
                    }
                }
                break;
            case 13:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("multi_beneficiary_project")));
                break;
            case 14:
                dist.getHost().setStorage_type(text);
                break;
            case 15:
            case 16:
                ds.getTechnicalResource().add(new TechnicalResource(text, new TextType_Id("software_tools")));
                break;
            case 17:
                ds.getTechnicalResource().add(new TechnicalResource("relevant software included: " + text,
                        new TextType_Id("software_tools")));
                break;
            case 18:
                dist.getHost().setTitle(text);
                break;
            case 19:
                dist.getHost().setDescription("Appropriate arrangements with the identified repository explored:" + text);
                break;
            case 20:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("access_provided")));
                break;
            case 21:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("data_access_committee_needed")));
                break;
            case 22:
            case 23:
                ds.getSecurityAndPrivacy().add(new SecurityAndPrivacy("AccessAuthorization", text));
                break;
            case 26:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("standard_vocabulary")));
                break;
            case 27:
                ds.getMetadata().add(new Metadata(text, new TextType_Id("ontology_mappings")));
                break;
        }
    }

    private void setContact(Plan p, DMP dmp) {
        if (!p.getPrincipal_investigator().isEmpty()) {
            Contact_Id cid = null;
            if (!p.getPrincipal_investigator_identifier().isEmpty()) {
                cid = new Contact_Id(p.getPrincipal_investigator_identifier());
                cid.setContact_id_type(TypeIdentifier.checkType(cid.getContact_id()));
            }
            dmp.setContact(new Contact(
                    p.getPrincipal_investigator(),
                    p.getPrincipal_investigator_email(),
                    p.getPrincipal_investigator_phone(),
                    cid));
        }
    }

    private void setProject(Plan p, DMP dmp) {
        if (!p.getTitle().isEmpty()) {
            Project project = new Project(p.getTitle(), p.getDescription());
            project.setFunding(getFunding(p));
            List<Project> projectList = new ArrayList<>();
            projectList.add(project);
            dmp.setProject(projectList);
        }
    }

    private Funding getFunding(Plan p) {
        Funding funding = new Funding();
        if (!p.getFunder_name().isEmpty()) {
            funding.setFunderID(p.getFunder_name());
        }
        if (!p.getGrant_number().isEmpty()) {
            funding.setGrantID(p.getGrant_number());
        }
        return funding;
    }
}