package aws.service;

import java.util.List;

import aws.model.AWSResource;
import dap.sportal.bds.azure.model.Metric;
import dap.sportal.bds.azure.model.ServiceRequest;
import dap.sportal.bds.azure.model.UseResource;
import dap.sportal.bds.monitoring.metric.model.Monitoring;
/**
 * <pre>
 * 본 클래스는 AWS API Call을 관리하기위한 Service 인터페이스입니다.
 * </pre>
 * @author A41888
 *
 */
public interface AWSService {
	/**
	 * 인스턴스 시작
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 리소스명(
	 */
	public void startInstances(String resGroupId, String instanceId);
	/**
	 * 인스턴스 중지
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 리소스명(
	 */
	public void stopInstances(String resGroupId, String instanceId);
	/**
	 * 인스턴스 종료
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 리소스명
	 */
	public void terminateInstances(String resGroupId, String instanceId);
	/**
	 * 인스턴스 종료
	 * @param resGroupId 리소스그룹Id
	 * @param instanceIdList 인스턴스ID목록
	 */
	public void terminateInstances(String resGroupId, List<String> instanceIdList);
	/**
	 * 해당 리소스그룹이 있는 계정에 인스턴스 정보 조회
	 * @param resGroupId 리소스그룹ID
	 * @return List<AWSResource> 인스턴스 정보 목록
	 */
	public List<AWSResource> describeInstances(String resGroupId);
	/**
	 * EMR 클러스터 목록 조회
	 * @param resGroupId 리소스그룹ID
	 * @return List<AWSResource> EMR 클러스터 목록
	 */
	public List<AWSResource> listClusters(String resGroupId);
	/**
	 * EMR 클러스터에 인스턴스 목록 조회
	 * @param resGroupId 리소스그룹ID
	 * @param resourceId 리소스Id = EMR 클러스터 Id
	 * @return List<AWSResource> EMR 클러스터에 인스턴스 목록
	 */
	public List<AWSResource> listInstances(String resGroupId, String resourceId);
	/**
	 * 리소스에 tag 생성
	 * @param resGroupId 리소스그룹Id
	 * @param solutionReqId 솔루션요청Id
	 * @param resourceId 리소스Id
	public void createTags(String resGroupId, String solutionReqId, String resourceId);
	 */	
	/**
	 * 모니터랑 데이터 조회
	 * @param resGroupId 리소스그룹Id
	 * @param instanceIdList 인스턴스Id목록
	 * @return Monitoring 모니터링 데이터
	 */
	public Monitoring getMetricData(String resGroupId, List<String> instanceIdList);
	/**
	 * one VM - one metricNm 24시간전부터 1시간단위 통계 조회 
	 * @param resGroupId 리소스그룹Id
	 * @param instanceId 인스턴스Id
	 * @param metricNm 지표 CPUUtilization NetworkIn NetworkOut DiskReadBytes DiskWriteBytes
	 * @param Statistics 값유형 Minimum, Maximum, Average, SampleCount, Sum
	 * @return List<Metric> 통계값
	 */
	public List<Metric> getMetricStatistics(String resGroupId, String instanceId, String metricNm, String statistics);
	/**
	 * instance 상태 조회
	 * @param resGroupId 리소스그룹ID
	 * @param instanceIdList 인스턴스ID목록
	 * @return List<UseResource> 인스턴스 상태 목록
	 */
	public List<UseResource> describeInstanceStatus(String resGroupId, List<UseResource> instanceIdList);
	/**
	 * instance 상태 세부 조회
	 * @param resGroupId 리소스그룹ID
	 * @param instanceIdList 인스턴스ID목록
	 * @return List<AwsResource> 인스턴스 상태 목록
	 */
	public List<AWSResource> describeInstanceStatusDetail(String resGroupId, List<String> instanceIdList);
	/**
	 * S3 bucket 목록 조회
	 * @param resGroupId 리소스그룹ID
	 * @return
	 */
	public List<AWSResource> listAllMyBuckets(String resGroupId);
	/**
	 * 사용량 레포트 가져와서 DB에 적재
	 * @param yyyymm 년월 ex)2018-04
	 * @return int 데이터 생성 결과
	 */
	public int getUsageReport(String yyyymm);
	/**
	 * AWS 사용량 집계
	 * @param yyyymm 년월 ex)2018-04
	 */
	public void aggregatesDailyUsage(String yyyymm);
	/**
	 * AWS 플랫폼 삭제
     * @param serviceDeleteReqId 삭제 시퀀스 아이디
     * @param deleteTp 삭제타입
     * @param serviceRequest 서비스 요청정보
	 */
	public void removePlatformResources(String serviceDeleteReqId, String deleteTp, ServiceRequest serviceRequest);
}
