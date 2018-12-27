package aws;

import java.util.HashMap;
import java.util.Map;

public class AWSResource {
	private String resourceType;//instance, vpc, subnet, esb.....
	private String resourceId;
	private String attachInstanceId;
	//instance
	private String instanceType;
	private String architecture;
	private String platform;
	private String instanceState;
	private String privateIpAddress;
	private String publicIpAddress;
	private String checkSystemStatus;//부팅후 시스템 상태 검사
	private String checkInstanceStatus;//부팅후 인스턴스 상태 검사
	//EMR
	private String clusterId;
	private String clusterState;
	//ETC
	private String name;//user::Name Tag
	
	private Map<String, String> tagSet;

	public String getInstanceType() {
		return instanceType;
	}
	public String getAttachInstanceId() {
		return attachInstanceId;
	}
	public void setAttachInstanceId(String attachInstanceId) {
		this.attachInstanceId = attachInstanceId;
	}
	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}
	public String getArchitecture() {
		return architecture;
	}
	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}
	public String getPlatform() {
		return platform;
	}
	public String getPrivateIpAddress() {
		return privateIpAddress;
	}
	public void setPrivateIpAddress(String privateIpAddress) {
		this.privateIpAddress = privateIpAddress;
	}
	public String getPublicIpAddress() {
		return publicIpAddress;
	}
	public void setPublicIpAddress(String publicIpAddress) {
		this.publicIpAddress = publicIpAddress;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public String getResourceType() {
		return resourceType;
	}
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	public String getResourceId() {
		return resourceId;
	}
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	public String getInstanceState() {
		return instanceState;
	}
	public void setInstanceState(String instanceState) {
		this.instanceState = instanceState;
	}
	public String getClusterId() {
		return clusterId;
	}
	public void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}
	public String getClusterState() {
		return clusterState;
	}
	public void setClusterState(String clusterState) {
		this.clusterState = clusterState;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCheckSystemStatus() {
		return checkSystemStatus;
	}
	public void setCheckSystemStatus(String checkSystemStatus) {
		this.checkSystemStatus = checkSystemStatus;
	}
	public String getCheckInstanceStatus() {
		return checkInstanceStatus;
	}
	public void setCheckInstanceStatus(String checkInstanceStatus) {
		this.checkInstanceStatus = checkInstanceStatus;
	}
	public void setTagSet(Map<String, String> tagSet) {
		this.tagSet = tagSet;
	}
	public Map<String, String> getTagSet() {
		return tagSet;
	}
	
	public void addTag(String tagNm, String tagVal) {
		if( tagSet == null ) {
			tagSet = new HashMap<String, String>();
		}
		tagSet.put(tagNm, tagVal);
	}
	public String getTag(String tagNm) {
		String tagVal = null;
		if( tagSet != null ) {
			tagVal = tagSet.get(tagNm);
		}
		return tagVal;
	}
	public String getTagSetToString() {
		StringBuilder sb = new StringBuilder("");
		if( tagSet != null ) {
			for (Map.Entry<String, String> entrySet : tagSet.entrySet()) {
				if(sb.length() > 0 ) { sb.append(","); }
				sb.append(entrySet.getKey()).append(":").append("'").append(entrySet.getValue()).append("'");
			}
			sb.append("}").insert(0,"{");
		}
		return sb.toString();
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AWSResource [resourceType=").append(resourceType)
				.append(", resourceId=").append(resourceId)
				.append(", attachInstanceId=").append(attachInstanceId)
				.append(", instanceType=").append(instanceType)
				.append(", architecture=").append(architecture)
				.append(", platform=").append(platform)
				.append(", instanceState=").append(instanceState)
				.append(", privateIpAddress=").append(privateIpAddress)
				.append(", publicIpAddress=").append(publicIpAddress)
				.append(", checkSystemStatus=").append(checkSystemStatus)
				.append(", checkInstanceStatus=").append(checkInstanceStatus)
				.append(", clusterId=").append(clusterId)
				.append(", clusterState=").append(clusterState)
				.append(", name=").append(name)
				.append(", tagSet=").append(getTagSetToString()).append("}");
		return builder.toString();
	}
}
